<template>
  <div>
    <nav-bar-menu active-mode="vocadb" />
    <b-row class="col-12 m-0">
      <b-toaster class="b-toaster-top-center" name="toaster-2"></b-toaster>
      <b-col>
        <div style="display: flex; align-items: center">
          <b-container class="col-lg-11">
            <b-tabs v-model="browseMode" class="mt-3" content-class="mt-3">
              <b-tab title="Browse via song entries" active>
                <EntriesBySongEntries :mode="browseMode" :this-mode="0" />
              </b-tab>
              <b-tab title="Browse via activity entries">
                <entries-by-activity-entries
                  :mode="browseMode"
                  :this-mode="1"
                />
              </b-tab>
            </b-tabs>
          </b-container>
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";

import NavBarMenu from "@/components/NavBarMenu.vue";
import EntriesBySongEntries from "@/components/pages/EntriesBySongEntries.vue";
import EntriesByActivityEntries from "@/components/pages/EntriesByActivityEntries.vue";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({
  components: { NavBarMenu, EntriesBySongEntries, EntriesByActivityEntries }
})
export default class extends Vue {
  private browseMode = 0;
}
</script>
