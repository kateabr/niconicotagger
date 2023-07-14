<template>
  <span>
    <span v-if="description.length > 0">
      <b-button
        v-b-toggle="contentId + '_description'"
        variant="link"
        size="sm"
        class="py-0"
        ><font-awesome-icon
          class="mr-sm-1"
          icon="fas fa-angle-down"
        />Description</b-button
      >
      <b-collapse :id="contentId + '_description'" class="collapsed mt-2">
        <b-card v-cloak>
          <span v-html="description" />
        </b-card>
      </b-collapse>
    </span>
    <span v-else class="text-secondary">
      <small>Could not extract description</small>
    </span>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly contentId!: string;
  @Prop()
  private readonly description!: string;
  @Prop()
  private readonly forceCollapse!: boolean;
}
</script>
