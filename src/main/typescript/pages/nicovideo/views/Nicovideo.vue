<template>
  <div>
    <b-toaster class="b-toaster-top-center" name="toaster-nicovideo" />
    <nav-bar-menu active-mode="nicovideo" />
    <div style="display: flex; align-items: center">
      <b-container class="col-lg-11">
        <b-tabs v-model="browseMode" class="mt-3" content-class="mt-3">
          <b-tab title="Browse by NicoNicoDouga tag" active>
            <videos-by-mapped-nico-nico-tag :mode="browseMode" this-mode="0" />
          </b-tab>
          <b-tab title="Browse by VocaDB tag">
            <videos-by-voca-db-tag-mappings :mode="browseMode" this-mode="1" />
          </b-tab>
        </b-tabs>
      </b-container>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import NavBarMenu from "@/components/NavBarMenu.vue";
import VideosByMappedNicoNicoTag from "@/components/pages/VideosByMappedNicoNicoTag.vue";
import VideosByVocaDbTagMappings from "@/components/pages/VideosByVocaDbTagMappings.vue";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({
  components: {
    NavBarMenu,
    VideosByMappedNicoNicoTag,
    VideosByVocaDbTagMappings
  }
})
export default class extends Vue {
  private browseMode = 0;
}
</script>
