<template>
  <div>
    <error-message
      :alert-status-text="alertStatusText"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <b-row>
      <b-col>
        <div v-if="clientType != ClientType.UNKNOWN" class="mt-3">
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
                <b-card-body>
                  <b-row v-for="rows in 3">
                    <b-col v-for="cols in 3" class="my-2">
                      <b-skeleton-wrapper :loading="eventPreviews.length == 0">
                        <template #loading>
                          <b-card no-body img-left>
                            <b-skeleton-img
                              card-img="left"
                              :width="imageWidth"
                            ></b-skeleton-img>
                            <b-card-body>
                              <b-skeleton width="100%"></b-skeleton>
                              <b-skeleton width="20%"></b-skeleton>
                              <b-skeleton width="60%"></b-skeleton>
                            </b-card-body>
                          </b-card>
                        </template>
                      </b-skeleton-wrapper>
                    </b-col>
                  </b-row>
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
            :event-previews="eventPreviews"
          />
          <div v-else>
            <h4 class="text-muted">Failed to load upcoming events</h4>
          </div>
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { api } from "@/backend";
import Component from "vue-class-component";
import { Prop } from "vue-property-decorator";
import {
  getErrorData,
  formatDate,
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
  private locale = "";
  private eventPreviews: ReleaseEventPreview[] = [];

  private failedToLoadPreviews = false;
  private readonly imageWidth = "225px";

  private alertMessage = "";
  private alertStatusText = "";

  private getStatusColor(eventPreview: ReleaseEventPreview): string {
    if (eventPreview.status == "ONGOING") {
      return "success";
    } else if (eventPreview.status == "ENDED") {
      return "dark";
    } else {
      return "light";
    }
  }

  private async loadEventPreviews(): Promise<void> {
    try {
      const response = await api.loadEventPreviews(this.clientType);
      this.eventPreviews = response.map(eventPreview => {
        return {
          id: eventPreview.id,
          name: eventPreview.name,
          category: eventPreview.category,
          date: formatDate(eventPreview.date, this.locale),
          endDate: formatDate(eventPreview.endDate, this.locale),
          dateString: formatDateString(
            eventPreview.date,
            eventPreview.endDate,
            this.locale
          ),
          status: eventPreview.status,
          pictureUrl: eventPreview.pictureUrl
        };
      });
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
    this.locale = navigator.language;
    this.loadEventPreviews();
  }
}
</script>

<style lang="scss">
@import "../style.scss";
</style>
