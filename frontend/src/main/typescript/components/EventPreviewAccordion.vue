<template>
  <div class="accordion" role="tablist">
    <b-card no-body>
      <b-card-header header-tag="header" class="p-1" role="tab">
        <b-button v-b-toggle.ongoing block variant="link"
          >Happening now</b-button
        >
      </b-card-header>
      <b-collapse id="ongoing" visible accordion="my-accordion" role="tabpanel">
        <b-card-body>
          <event-preview-stack
            :client-type="clientType"
            :event-previews="
              eventPreviews.filter(event => event.status == 'ONGOING')
            "
          />
        </b-card-body>
      </b-collapse>
    </b-card>

    <b-card no-body>
      <b-card-header header-tag="header" class="p-1" role="tab">
        <b-button v-b-toggle.upcoming block variant="link">Upcoming</b-button>
      </b-card-header>
      <b-collapse id="upcoming" accordion="my-accordion" role="tabpanel">
        <b-card-body>
          <event-preview-stack
            :client-type="clientType"
            :event-previews="
              eventPreviews.filter(event => event.status == 'UPCOMING')
            "
          />
        </b-card-body>
      </b-collapse>
    </b-card>

    <b-card no-body>
      <b-card-header header-tag="header" class="p-1" role="tab">
        <b-button v-b-toggle.ended block variant="link"
          >Recently ended</b-button
        >
      </b-card-header>
      <b-collapse id="ended" accordion="my-accordion" role="tabpanel">
        <b-card-body>
          <event-preview-stack
            :client-type="clientType"
            :event-previews="
              eventPreviews.filter(event => event.status == 'ENDED')
            "
          />
        </b-card-body>
      </b-collapse>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { ClientType } from "@/backend/dto/enumeration";
import { ReleaseEventPreview } from "@/backend/dto/response/releaseEventPreview";
import EventPreviewStack from "@/components/EventPreviewStack.vue";

@Component({ components: { EventPreviewStack } })
export default class extends Vue {
  @Prop()
  private readonly eventPreviews!: ReleaseEventPreview[];

  @Prop()
  private readonly clientType!: ClientType;
}
</script>
