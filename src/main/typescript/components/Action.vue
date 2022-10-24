<template>
  <div>
    <ol v-if="processItem">
      <li v-if="mode === 'Assign'">
        Set "
        <b-link :href="link" target="_blank">{{ name }} </b-link>
        " as release event
      </li>
      <li v-else-if="mode === 'TagWithParticipant'">
        Tag with "
        <b-link target="_blank" :href="participantLink"
          >event participant</b-link
        >
        " and update description
      </li>
      <li v-else-if="mode === 'TagWithMultiple'">
        Tag with "
        <b-link target="_blank" :href="multipleEventsLink"
          >multiple events</b-link
        >
        " and update description
      </li>
      <li v-else-if="mode === 'CheckDescription'">
        <span class="text-danger text-monospace">Important:</span>
        check that description mentions current event
      </li>
      <li v-if="tagToRemove !== undefined">
        Remove tag "<b-link :href="tagToRemoveLink" target="_blank">{{
          tagToRemove
        }}</b-link
        >"
      </li>
    </ol>
    <ol v-else-if="mode === 'NeedToRemove'">
      <li>
        <span class="text-danger text-monospace">Important:</span>
        need to change or remove the release event because song was not first
        published during "
        <b-link :href="link" target="_blank">{{ name }}</b-link>
        "
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
  private readonly mode!:
    | "Assign"
    | "TagWithParticipant"
    | "TagWithMultiple"
    | "CheckDescription"
    | "NeedToRemove"
    | "NoAction";

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

  @Prop()
  private readonly participantLink!: string;

  @Prop()
  private readonly multipleEventsLink!: string;
}
</script>
