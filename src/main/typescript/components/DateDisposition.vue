<template>
  <div>
    <font-awesome-icon icon="fa-solid fa-calendar" class="mr-1" />{{
      getReleaseDateFormatted(releaseDate)
    }}
    <b-badge
      :variant="getDispositionBadgeColorVariant(eventDateComparison)"
      class="mr-1 ml-3"
    >
      {{ eventDateComparison.disposition }}
    </b-badge>
    <span
      v-if="
        eventDateComparison.disposition !== 'unknown' &&
        eventDateComparison.disposition !== 'perfect'
      "
    >
      {{ getReleaseDateCommentaryFormatted(eventDateComparison) }}
    </span>
    <b-badge
      v-if="eventDateComparison.participated"
      variant="success"
      class="mr-1 ml-3"
    >
      event participant
    </b-badge>
    <b-badge
      v-else-if="eventDateComparison.participatedOnUpload"
      variant="success"
      class="mr-1 ml-3"
    >
      participated on upload
    </b-badge>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { getDispositionBadgeColorVariant, DateComparisonResult } from "@/utils";
import { DateTime } from "luxon";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly releaseDate!: string;

  @Prop()
  private readonly eventDateComparison!: DateComparisonResult;

  @Prop()
  private readonly delta!: number;

  private getDispositionBadgeColorVariant(
    eventDateComparison: DateComparisonResult
  ): string {
    return getDispositionBadgeColorVariant(eventDateComparison, this.delta);
  }

  private getReleaseDateFormatted(releaseDate: string): string {
    return DateTime.fromISO(releaseDate).toLocaleString();
  }

  private getReleaseDateCommentaryFormatted(
    eventDateComparison: DateComparisonResult
  ): string {
    if (eventDateComparison.disposition == "perfect") {
      return "";
    }
    return "(by " + eventDateComparison.dayDiff + "day(s))";
  }
}
</script>
