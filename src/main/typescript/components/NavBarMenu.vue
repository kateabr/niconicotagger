<template>
  <b-navbar print toggleable="lg" variant="faded" type="light">
    <b-navbar-brand tag="h1" href="/login">NicoNicoTagger</b-navbar-brand>

    <b-navbar-toggle target="nav-collapse"></b-navbar-toggle>

    <b-collapse id="nav-collapse" is-nav>
      <b-navbar-nav>
        <b-nav-item :active="activeMode === 'tags'" href="/tags"
          >Tags</b-nav-item
        >
        <b-nav-item :active="activeMode === 'events'" href="/events"
          >Events</b-nav-item
        >
      </b-navbar-nav>
      <b-navbar-nav class="ml-auto">
        <b-nav-item
          href="https://github.com/kateabr/niconicotagger"
          target="_blank"
          ><font-awesome-icon
            icon="fas fa-arrow-right"
            class="mr-1"
          />GitHub</b-nav-item
        >
      </b-navbar-nav>
    </b-collapse>
  </b-navbar>
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
