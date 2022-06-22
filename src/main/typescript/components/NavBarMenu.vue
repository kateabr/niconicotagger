<template>
  <b-row class="nav nav-pills nav-fill mt-3">
    <b-col cols="4"></b-col>
    <b-col class="m-auto">
      <b-link
        :class="
          'nav-link btn-block text-center' +
          (activeMode === 'nicovideo' ? ' active' : '')
        "
        to="nicovideo"
        >NicoNicoDouga</b-link
      >
    </b-col>
    <b-col class="m-auto">
      <b-link
        :class="
          'nav-link btn-block text-center' +
          (activeMode === 'vocadb' ? ' active' : '')
        "
        to="vocadb"
        >VocaDB</b-link
      >
    </b-col>
    <b-col class="m-auto">
      <b-link
        :class="
          'nav-link btn-block text-center' +
          (activeMode === 'events' ? ' active' : '')
        "
        to="events"
        >Events</b-link
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
