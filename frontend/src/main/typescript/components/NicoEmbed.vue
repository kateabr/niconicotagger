<template>
  <b-card v-cloak class="embed-responsive embed-responsive-16by9">
    <iframe
      class="embed-responsive-item"
      allowfullscreen="allowfullscreen"
      style="border: none"
      :src="getNicoEmbedUrl(contentId)"
    />
  </b-card>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { getNicoEmbedUrl } from "@/utils";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly contentId!: string;

  private getNicoEmbedUrl(contentId: string): string {
    return getNicoEmbedUrl(contentId);
  }
}
</script>
