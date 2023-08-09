<template>
  <div>
    <ol v-if="processItem">
      <li>
        Add "<b-link :href="link" target="_blank">{{ name }}</b-link
        >" to release events
      </li>
      <li v-if="tagToRemove !== undefined">
        Remove tag "<b-link :href="tagToRemoveLink" target="_blank">{{
          tagToRemove
        }}</b-link
        >"
      </li>
    </ol>
    <div v-if="!eligible" class="text-muted">
      <font-awesome-icon icon="fa-solid fa-circle-exclamation" class="mr-1" />
      Entry may be ineligible for participation
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly eligible!: boolean;

  @Prop()
  private readonly processItem!: boolean;

  @Prop()
  private readonly tagToRemove: string | undefined;

  @Prop()
  private readonly tagToRemoveLink: string | undefined;

  @Prop()
  private readonly name!: string;

  @Prop()
  private readonly link!: string;
}
</script>
