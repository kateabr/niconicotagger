<template>
  <div>
    <error-message
      :alert-status-text="alertStatusText"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <b-row>
      <b-col>
        <div v-if="clientType != unknownClientType" class="mt-2">
          <b-row class="mb-3">
            <b-col>
              <template>
                <b-input-group
                  inline
                  :state="validateEventScopeState()"
                  invalid-feedback="Invalid scope value"
                >
                  <template #prepend>
                    <b-input-group-text class="justify-content-center"
                      >Scope (in days):
                    </b-input-group-text>
                  </template>
                  <template>
                    <b-form-input
                      id="scope-form"
                      v-model.number="eventScopeDays"
                      type="number"
                      :disabled="
                        eventScopeDays == null || defaultDisableCondition()
                      "
                      aria-describedby="input-live-help input-live-feedback"
                      :state="validateEventScopeState()"
                      @keydown.enter.native="
                        validateEventScopeState()
                          ? loadEventPreviews(true)
                          : null
                      "
                    >
                    </b-form-input>
                  </template>
                </b-input-group>
              </template>
            </b-col>
            <b-col class="my-auto">
              <b-form-checkbox
                v-model="hideOffline"
                class="text-muted"
                :disabled="defaultDisableCondition()"
              >
                Hide offline events
              </b-form-checkbox>
            </b-col>
          </b-row>
          <div
            v-if="eventPreviews.length == 0 && !failedToLoadPreviews"
            class="accordion"
            role="tablist"
          >
            <b-card no-body>
              <b-card-header header-tag="header" class="p-1" role="tab">
                <b-button v-b-toggle.ongoing disabled block variant="link"
                  >Happening now</b-button
                >
              </b-card-header>
              <b-collapse
                id="ongoing"
                visible
                accordion="my-accordion"
                role="tabpanel"
              >
                <b-card-body class="text-center text-muted">
                  <b-spinner></b-spinner>
                </b-card-body>
              </b-collapse>
            </b-card>

            <b-card no-body>
              <b-card-header header-tag="header" class="p-1" role="tab">
                <b-button v-b-toggle.upcoming disabled block variant="link"
                  >Upcoming</b-button
                >
              </b-card-header>
              <b-collapse
                id="upcoming"
                accordion="my-accordion"
                role="tabpanel"
              >
                <b-card-body />
              </b-collapse>
            </b-card>

            <b-card no-body>
              <b-card-header header-tag="header" class="p-1" role="tab">
                <b-button v-b-toggle.ended disabled block variant="link"
                  >Recently ended</b-button
                >
              </b-card-header>
              <b-collapse id="ended" accordion="my-accordion" role="tabpanel">
                <b-card-body />
              </b-collapse>
            </b-card>
          </div>
          <event-preview-accordion
            v-else-if="!failedToLoadPreviews"
            :client-type="clientType"
            :event-previews="
              eventPreviews.filter(
                preview => !hideOffline || !preview.isOffline
              )
            "
          />
          <div v-else>
            <h4 class="text-muted">Failed to load upcoming events</h4>
          </div>
        </div>
      </b-col>
    </b-row>
    <b-button
      class="mt-3"
      size="sm"
      block
      variant="link"
      :disabled="!validateEventScopeState()"
      @click="loadEventPreviews(false)"
      ><font-awesome-icon
        icon="fa-solid fa-arrow-rotate-right"
        class="mr-1"
      />Reload</b-button
    >
    <div v-if="loadedAt != null" class="text-muted text-center small mt-3">
      Loaded at {{ loadedAt }}
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { api } from "@/backend";
import Component from "vue-class-component";
import { Prop } from "vue-property-decorator";
import {
  getErrorData,
  formatDateString,
  getNicoVideoUrl,
  getShortenedSongType,
  getUniqueElementId,
  getVocaDBEventUrl,
  getClientType
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import NicoEmbed from "@/components/NicoEmbed.vue";
import DateDisposition from "@/components/DateDisposition.vue";
import { AxiosError, AxiosResponse } from "axios";
import Action from "@/components/Action.vue";
import NicoDescription from "@/components/NicoDescription.vue";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import { ReleaseEventPreview } from "@/backend/dto/response/releaseEventPreview";
import EventPreviewStack from "@/components/EventPreviewStack.vue";
import EventPreviewAccordion from "@/components/EventPreviewAccordion.vue";
import { unknownClientType } from "@/constants";

@Component({
  computed: {
    unknownClientType() {
      return unknownClientType;
    }
  },
  methods: {
    getNicoVideoUrl,
    getVocaDBEventUrl
  },
  components: {
    EventPreviewAccordion,
    EventPreviewStack,
    EntryErrorReport,
    NicoDescription,
    Action,
    ErrorMessage,
    NicoEmbed,
    DateDisposition
  }
})
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly targName: string | undefined;

  private clientType: string = getClientType();
  private eventPreviews: ReleaseEventPreview[] = [];

  private loadedAt: string | null = null;
  private hideOffline = true;
  private failedToLoadPreviews = false;
  private eventScopeDays: number | null = null;

  private alertMessage = "";
  private alertStatusText = "";

  private validateEventScopeState(): boolean {
    return (
      this.eventScopeDays == null ||
      (this.eventScopeDays >= 1 && this.eventScopeDays <= 365)
    );
  }

  private defaultDisableCondition(): boolean {
    return this.eventPreviews.length == 0 && !this.failedToLoadPreviews;
  }

  private async loadEventPreviews(useCached: boolean): Promise<void> {
    this.eventPreviews = [];
    this.loadedAt = null;
    try {
      const response = await api.loadEventPreviews({
        clientType: this.clientType,
        useCached: useCached,
        eventScopeDays: this.eventScopeDays
      });
      this.eventPreviews = response.eventPreviews.map(eventPreview => {
        return {
          id: eventPreview.id,
          name: eventPreview.name,
          category: eventPreview.category,
          date: eventPreview.date,
          endDate: eventPreview.endDate,
          dateString: formatDateString(eventPreview.date, eventPreview.endDate),
          status: eventPreview.status,
          pictureUrl: eventPreview.pictureUrl,
          isOffline: eventPreview.isOffline
        };
      });
      this.eventScopeDays = response.eventScopeDays;
      this.loadedAt = new Date().toLocaleString();
      this.failedToLoadPreviews = false;
    } catch (err) {
      this.processError((err as AxiosError).response);
      this.failedToLoadPreviews = true;
    }
  }

  // error handling
  private processError(response: AxiosResponse | undefined): void {
    const errorData = getErrorData(response);
    this.alertMessage = errorData.message;
    this.alertStatusText = errorData.statusText;
    this.$bvToast.show(getUniqueElementId("error_", this.thisMode.toString()));
  }

  created(): void {
    this.clientType = getClientType();
    this.loadEventPreviews(true);
  }
}
</script>

<style lang="scss">
@import "../style.scss";
</style>
