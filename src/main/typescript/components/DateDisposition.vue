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
      >(by
      {{ eventDateComparison.dayDiff }}
      day(s))
    </span>
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

  private getDispositionBadgeColorVariant(
    eventDateComparison: DateComparisonResult
  ): string {
    return getDispositionBadgeColorVariant(eventDateComparison);
  }

  private getReleaseDateFormatted(releaseDate: string): string {
    return DateTime.fromISO(releaseDate).toLocaleString();
  }
}
</script>
