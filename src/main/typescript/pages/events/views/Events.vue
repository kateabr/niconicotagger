<template>
  <div>
    <nav-bar-menu active-mode="events" />
    <b-toaster class="b-toaster-top-center" name="toaster-events" />
    <div style="display: flex; align-items: center">
      <b-container class="col-lg-11">
        <b-tabs v-model="browseMode" class="mt-3" content-class="mt-3">
          <b-tab title="Browse by event tag (VocaDB)">
            <event-by-voca-db-tag-tab :mode="browseMode" this-mode="0" />
          </b-tab>
          <b-tab title="Browse by event tag (NND)">
            <event-by-nnd-tag-tab :mode="browseMode" this-mode="1" />
          </b-tab>
        </b-tabs>
      </b-container>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import VueClipboard from "vue-clipboard2";
import NavBarMenu from "@/components/NavBarMenu.vue";
import EventByVocaDbTagTab from "@/components/pages/EventByVocaDbTagTab.vue";
import EventByNndTagTab from "@/components/pages/EventByNndTagTab.vue";

Vue.use(VueClipboard);

@Component({ components: { NavBarMenu, EventByVocaDbTagTab, EventByNndTagTab } })
export default class extends Vue {
  private browseMode = 0;
}
</script>
