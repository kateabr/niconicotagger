<template>
  <div>
    <ol v-if="processItem">
      <li v-if="hasAction('RemoveEvent')">
        <span class="text-danger text-monospace">Important:</span>
        release event "<b-link :href="link" target="_blank">{{ name }}</b-link
      >" must be removed because song was first published
        outside its time frame
      </li>
      <li v-if="hasAction('Assign')">
        Add "<b-link :href="link" target="_blank">{{ name }}</b-link
        >" to release events
      </li>
      <li v-else-if="hasAction('TagWithParticipant')">
        Tag with "<b-link target="_blank" :href="participantLink"
          >event participant</b-link
        >"
      </li>
      <li v-if="hasAction('UpdateDescription')">Update entry description</li>
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
import { EntryAction, hasAction } from "@/utils";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly entryActions!: EntryAction[];

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

  @Prop()
  private readonly eventLinkInDescription!: boolean;

  private hasAction(action: string): boolean {
    return hasAction(this.entryActions, action);
  }
}
</script>
