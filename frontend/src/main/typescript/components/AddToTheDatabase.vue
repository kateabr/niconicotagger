<template>
  <span>
    <b-button
      size="sm"
      :disabled="disabled"
      :href="getVocaDBAddSongUrl(videoId)"
      target="_blank"
      >Add to the database
    </b-button>
    <div
      v-if="publisher != null && publisher.type == 'DATABASE'"
      class="small text-secondary"
    >
      Published by
      <b-link target="_blank" :href="publisher.link">{{
        publisher.name
      }}</b-link>
    </div>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { PublisherInfo } from "@/backend/dto/lowerLevelStruct";
import { getVocaDBAddSongUrl } from "@/utils";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly disabled!: boolean;

  @Prop()
  private readonly publisher!: PublisherInfo | null;

  @Prop()
  private readonly videoId!: string;

  @Prop()
  private readonly clientType!: string;

  private getVocaDBAddSongUrl(contentId: string): string {
    return getVocaDBAddSongUrl(this.clientType, contentId);
  }
}
</script>
