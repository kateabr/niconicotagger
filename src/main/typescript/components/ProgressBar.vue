<template>
  <div class="flex-fill fixed-top mb-3">
    <b-progress
      height="5px"
      :value="distinctSongCount"
      :max="maxResults"
      :precision="0"
      :animated="fetching"
      striped
    />
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly distinctSongCount!: number;

  @Prop()
  private readonly maxResults!: number;

  @Prop()
  private readonly fetching!: boolean;
}
</script>
