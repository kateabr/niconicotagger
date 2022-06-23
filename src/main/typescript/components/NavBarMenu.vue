<template>
  <b-row class="nav nav-pills nav-fill mt-3">
    <b-col cols="4"></b-col>
    <b-col class="m-auto">
      <b-link
        :class="
          'nav-link btn-block text-center' +
          (activeMode === 'nicovideo' ? ' active' : '')
        "
        :disabled="activeMode === 'nicovideo'"
        to="nicovideo"
        ><font-awesome-icon
          icon="fas fa-tag"
          class="mr-1"
        />NicoNicoDouga</b-link
      >
    </b-col>
    <b-col class="m-auto">
      <b-link
        :class="
          'nav-link btn-block text-center' +
          (activeMode === 'vocadb' ? ' active' : '')
        "
        :disabled="activeMode === 'vocadb'"
        to="vocadb"
        ><font-awesome-icon icon="fas fa-tag" class="mr-1" />VocaDB</b-link
      >
    </b-col>
    <b-col class="m-auto">
      <b-link
        :class="
          'nav-link btn-block text-center' +
          (activeMode === 'events' ? ' active' : '')
        "
        :disabled="activeMode === 'events'"
        to="events"
        ><font-awesome-icon
          icon="fa-solid fa-calendar"
          class="mr-1"
        />Events</b-link
      >
    </b-col>
    <b-col cols="4"></b-col>
  </b-row>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly activeMode!: string;
}
</script>
