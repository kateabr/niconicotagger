<template>
  <b-navbar print toggleable="lg" variant="faded" type="light" class="bg-white">
    <b-navbar-brand tag="h1" href="/login">NicoNicoTagger 2.0</b-navbar-brand>

    <b-navbar-toggle target="nav-collapse"></b-navbar-toggle>

    <b-collapse id="nav-collapse" is-nav>
      <b-navbar-nav>
        <b-nav-item
          v-if="clientType != ClientType.UNKNOWN"
          :active="activeMode === 'tags'"
          href="/tags"
          >Tags</b-nav-item
        >
        <b-nav-item
          v-if="clientType != ClientType.UNKNOWN"
          :active="activeMode === 'events'"
          href="/events"
          >Events</b-nav-item
        >
        <b-nav-item
          v-if="clientType != ClientType.UNKNOWN"
          :active="activeMode === 'console'"
          href="/console"
          >Query console</b-nav-item
        >
      </b-navbar-nav>
      <b-navbar-nav class="ml-auto">
        <b-nav-text class="mr-2"
          ><span
            v-if="clientType !== ''"
            class="border rounded-sm border-info p-2 text-info bg-light"
            ><b-icon icon="key-fill" class="mr-1"></b-icon
            >{{ getDbName() }}</span
          >
          <span
            v-else
            class="border rounded-sm border-danger p-2 text-danger bg-light"
            ><b-iconstack class="mr-2">
              <b-icon
                stacked
                icon="key-fill"
                variant="danger"
                scale="0.80"
              ></b-icon>
              <b-icon
                stacked
                icon="slash-circle"
                variant="danger"
                scale="1.20"
              ></b-icon></b-iconstack
            >NOT LOGGED IN</span
          ></b-nav-text
        >
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
import { ClientType } from "@/backend/dto/enumeration";

@Component({
  computed: {
    ClientType() {
      return ClientType;
    }
  },
  components: {}
})
export default class extends Vue {
  @Prop()
  private readonly activeMode!: string;

  @Prop()
  private readonly clientType!: ClientType;

  getDbName(): string {
    return this.clientType.toUpperCase().split("_").join(" ");
  }
}
</script>
