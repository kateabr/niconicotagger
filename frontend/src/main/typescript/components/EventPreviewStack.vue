<template>
  <b-card-group columns>
    <b-card
      v-for="eventPreview in eventPreviews"
      :key="eventPreview.id"
      footer-class="p-0"
      no-body
      class="overflow-hidden"
    >
      <b-row no-gutters>
        <b-col
          v-if="eventPreview.pictureUrl != null"
          md="4"
          class="align-content-center"
        >
          <b-card-img
            :src="eventPreview.pictureUrl"
            :alt="eventPreview.name"
            class="rounded-0"
          ></b-card-img>
        </b-col>
        <b-col class="p-3">
          <b-card-text>
            <b-link
              :href="getVocaDBEventUrl(clientType, eventPreview.id)"
              target="_blank"
              ><h4 class="mb-0">
                {{ eventPreview.name }}
              </h4></b-link
            >
            <div class="text-muted mb-1">{{ eventPreview.category }}</div>
            <div>
              <font-awesome-icon icon="fa-solid fa-calendar" class="mr-1" />{{
                eventPreview.dateString
              }}
            </div>
          </b-card-text>
        </b-col>
        <b-col v-if="!eventPreview.isOffline" md="1">
          <b-button
            block
            variant="link"
            class="p-3"
            style="height: 100%"
            :to="{
              name: 'events-full',
              params: { browseMode: 'nicovideo', targName: eventPreview.name }
            }"
            target="_blank"
          >
            <font-awesome-icon icon="fas fa-angles-right" />
          </b-button>
        </b-col>
        <b-col v-else md="2">
          <div
            class="badge-secondary rounded-left mt-2 small text-center font-weight-bold"
          >
            OFFLINE
          </div>
        </b-col>
      </b-row>
    </b-card>
  </b-card-group>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { getVocaDBEventUrl } from "@/utils";
import { ReleaseEventPreview } from "@/backend/dto/response/releaseEventPreview";

@Component({
  methods: { getVocaDBEventUrl }
})
export default class extends Vue {
  @Prop()
  private readonly eventPreviews!: ReleaseEventPreview[];

  @Prop()
  private readonly clientType!: string;
}
</script>
