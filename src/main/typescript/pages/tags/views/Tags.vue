<template>
  <div>
    <nav-bar-menu active-mode="tags" />
    <b-row class="col-12 m-0">
      <b-toaster class="b-toaster-top-center" name="toaster-2"></b-toaster>
      <b-col>
        <div style="display: flex; align-items: center">
          <b-container class="col-lg-11">
            <b-nav tabs class="mb-2">
              <b-nav-item
                :to="{
                  name: 'tags',
                  params: { browseMode: 'activity-entries' }
                }"
                :active="browseMode === 'activity-entries'"
              >
                By activity entries
              </b-nav-item>
              <b-nav-item
                :to="{ name: 'tags', params: { browseMode: 'song-entries' } }"
                :active="browseMode === 'song-entries'"
              >
                By song entries
              </b-nav-item>
              <b-nav-item
                :to="{ name: 'tags', params: { browseMode: 'vocadb' } }"
                :active="browseMode === 'vocadb'"
              >
                By mapped VocaDB tag
              </b-nav-item>
              <b-nav-item
                :to="{ name: 'tags', params: { browseMode: 'nicovideo' } }"
                :active="browseMode === 'nicovideo'"
              >
                By mapped NND tag
              </b-nav-item>
            </b-nav>
            <div class="tab-content">
              <div
                :class="[
                  'tab-pane',
                  { active: browseMode === 'activity-entries' }
                ]"
              >
                <entries-by-activity-entries
                  :mode="browseMode"
                  this-mode="activity-entries"
                />
              </div>
              <div
                :class="['tab-pane', { active: browseMode === 'song-entries' }]"
              >
                <entries-by-song-entries
                  :mode="browseMode"
                  this-mode="song-entries"
                />
              </div>
              <div :class="['tab-pane', { active: browseMode === 'vocadb' }]">
                <videos-by-voca-db-tag-mappings
                  :mode="browseMode"
                  this-mode="vocadb"
                />
              </div>
              <div
                :class="['tab-pane', { active: browseMode === 'nicovideo' }]"
              >
                <videos-by-mapped-nico-nico-tag
                  :mode="browseMode"
                  this-mode="nicovideo"
                />
              </div>
            </div>
          </b-container>
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";

import NavBarMenu from "@/components/NavBarMenu.vue";
import EntriesBySongEntries from "@/components/pages/EntriesBySongEntries.vue";
import EntriesByActivityEntries from "@/components/pages/EntriesByActivityEntries.vue";
import VideosByVocaDbTagMappings from "@/components/pages/VideosByVocaDbTagMappings.vue";
import VideosByMappedNicoNicoTag from "@/components/pages/VideosByMappedNicoNicoTag.vue";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({
  components: {
    NavBarMenu,
    EntriesBySongEntries,
    EntriesByActivityEntries,
    VideosByVocaDbTagMappings,
    VideosByMappedNicoNicoTag
  }
})
export default class extends Vue {
  @Prop()
  private browseMode!:
    | "song-entries"
    | "activity-entries"
    | "vocadb"
    | "nicovideo";
}
</script>
