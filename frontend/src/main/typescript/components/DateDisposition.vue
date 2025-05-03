<template>
  <div>
    <span v-if="publishDate !== null">
      <font-awesome-icon icon="fa-solid fa-calendar" class="mr-1" />{{
        publishDate
      }}
      <b-badge :variant="disposition.color" class="mr-1 ml-3">
        {{ disposition.disposition }}
      </b-badge>
      <span v-if="disposition.disposition !== 'perfect'">
        (by {{ disposition.byDays }} day(s))
      </span>
    </span>
    <span v-else>
      <b-badge variant="danger">publish date not specified</b-badge>
    </span>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { DispositionRelativelyToDate } from "@/backend/dto/dateDisposition";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly disposition!: DispositionRelativelyToDate;

  @Prop()
  private readonly publishDate!: string | null;
}
</script>
