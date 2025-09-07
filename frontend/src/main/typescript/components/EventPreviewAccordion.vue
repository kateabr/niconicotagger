<template>
  <div class="accordion" role="tablist">
    <b-card no-body>
      <b-card-header header-tag="header" class="p-1" role="tab">
        <b-button v-b-toggle.ongoing block variant="link"
          >Happening now ({{ ongoingEvents.length }})</b-button
        >
      </b-card-header>
      <b-collapse id="ongoing" visible accordion="my-accordion" role="tabpanel">
        <b-card-body>
          <event-preview-stack
            :client-type="clientType"
            :event-previews="ongoingEvents"
          />
        </b-card-body>
      </b-collapse>
    </b-card>

    <b-card no-body>
      <b-card-header header-tag="header" class="p-1" role="tab">
        <b-button v-b-toggle.upcoming block variant="link"
          >Upcoming ({{ upcomingEvents.length }})</b-button
        >
      </b-card-header>
      <b-collapse id="upcoming" accordion="my-accordion" role="tabpanel">
        <b-card-body>
          <event-preview-stack
            :client-type="clientType"
            :event-previews="upcomingEvents"
          />
        </b-card-body>
      </b-collapse>
    </b-card>

    <b-card no-body>
      <b-card-header header-tag="header" class="p-1" role="tab">
        <b-button v-b-toggle.ended block variant="link"
          >Recently ended ({{ endedEvents.length }})</b-button
        >
      </b-card-header>
      <b-collapse id="ended" accordion="my-accordion" role="tabpanel">
        <b-card-body>
          <event-preview-stack
            :client-type="clientType"
            :event-previews="endedEvents"
          />
        </b-card-body>
      </b-collapse>
    </b-card>

    <b-card no-body>
      <b-card-header header-tag="header" class="p-1" role="tab">
        <b-button v-b-toggle.endless block variant="link"
          >Endless ({{ endlessEvents.length }})</b-button
        >
      </b-card-header>
      <b-collapse id="endless" accordion="my-accordion" role="tabpanel">
        <b-card-body>
          <event-preview-stack
            :client-type="clientType"
            :event-previews="endlessEvents"
          />
        </b-card-body>
      </b-collapse>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { ReleaseEventPreview } from "@/backend/dto/response/releaseEventPreview";
import EventPreviewStack from "@/components/EventPreviewStack.vue";

@Component({ components: { EventPreviewStack } })
export default class extends Vue {
  @Prop()
  private readonly eventPreviews!: ReleaseEventPreview[];

  @Prop()
  private readonly clientType!: string;

  private readonly ongoingEvents = this.eventPreviews.filter(
    event => event.status == "ONGOING"
  );
  private readonly upcomingEvents = this.eventPreviews.filter(
    event => event.status == "UPCOMING"
  );
  private readonly endedEvents = this.eventPreviews.filter(
    event => event.status == "ENDED"
  );
  private readonly endlessEvents = this.eventPreviews.filter(
    event => event.status == "ENDLESS"
  );
}
</script>
