<template>
  <b-card-group columns>
    <b-card
      v-for="eventPreview in eventPreviews"
      :key="eventPreview.id"
      footer-class="p-0"
      no-body
      class="overflow-hidden"
    >
      <template #footer>
        <b-button
          block
          variant="link"
          :to="{
            name: 'events-full',
            params: { browseMode: 'nicovideo', targName: eventPreview.name }
          }"
          target="_blank"
        >
          Browse
        </b-button>
      </template>
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
      </b-row>
    </b-card>
  </b-card-group>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { getVocaDBEventUrl } from "@/utils";
import { ClientType } from "@/backend/dto/enumeration";
import { ReleaseEventPreview } from "@/backend/dto/response/releaseEventPreview";

@Component({
  methods: { getVocaDBEventUrl }
})
export default class extends Vue {
  @Prop()
  private readonly eventPreviews!: ReleaseEventPreview[];

  @Prop()
  private readonly clientType!: ClientType;
}
</script>
