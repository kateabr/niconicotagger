<template>
  <div>
    <error-message
      :alert-status-text="alertStatusText"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <b-row>
      <b-col>
        <div v-if="clientType != ClientType.UNKNOWN" class="mt-2">
          <b-form-checkbox
            v-model="hideOffline"
            class="text-muted mb-3"
            :disabled="eventPreviews.length == 0"
          >
            Hide offline events
          </b-form-checkbox>
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
  getSongTypeColorForDisplay,
  getUniqueElementId,
  getVocaDBEventUrl,
  getClientType
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import NicoEmbed from "@/components/NicoEmbed.vue";
import DateDisposition from "@/components/DateDisposition.vue";
import { AxiosResponse } from "axios";
import Action from "@/components/Action.vue";
import NicoDescription from "@/components/NicoDescription.vue";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import { ClientType } from "@/backend/dto/enumeration";
import { ReleaseEventPreview } from "@/backend/dto/response/releaseEventPreview";
import EventPreviewStack from "@/components/EventPreviewStack.vue";
import EventPreviewAccordion from "@/components/EventPreviewAccordion.vue";

@Component({
  methods: {
    getShortenedSongType,
    getNicoVideoUrl,
    getSongTypeColorForDisplay,
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
  },
  computed: {
    ClientType() {
      return ClientType;
    }
  }
})
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly targName: string | undefined;

  private clientType: ClientType = getClientType();
  private eventPreviews: ReleaseEventPreview[] = [];

  private loadedAt: string | null = null;
  private hideOffline = true;
  private failedToLoadPreviews = false;

  private alertMessage = "";
  private alertStatusText = "";

  private async loadEventPreviews(useCached: boolean): Promise<void> {
    this.eventPreviews = [];
    this.loadedAt = null;
    try {
      const response = await api.loadEventPreviews({
        clientType: this.clientType,
        useCached: useCached
      });
      this.eventPreviews = response.map(eventPreview => {
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
      this.loadedAt = new Date().toLocaleString();
    } catch (err) {
      this.processError(err);
      this.failedToLoadPreviews = true;
    }
  }

  // error handling
  private processError(err: { response: AxiosResponse }): void {
    const errorData = getErrorData(err);
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
