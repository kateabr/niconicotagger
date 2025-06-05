<template>
  <div>
    <nav-bar-menu active-mode="console" :client-type="clientType" />
    <b-row class="col-12 m-0">
      <b-toaster class="b-toaster-top-center" name="toaster-2"></b-toaster>
      <b-col>
        <div style="display: flex; align-items: center">
          <b-container class="col-lg-11">
            <b-nav tabs class="mb-2">
              <b-nav-item
                :to="{
                  name: 'console-mode',
                  params: { browseMode: 'songs' }
                }"
                :active="browseMode === 'songs' || browseMode === ''"
              >
                Remove tags (songs)
              </b-nav-item>
              <b-nav-item
                :to="{
                  name: 'console-mode',
                  params: { browseMode: 'artists' }
                }"
                :active="browseMode === 'artists'"
              >
                Remove tags (artists)
              </b-nav-item>
            </b-nav>
            <div class="tab-content">
              <div :class="['tab-pane', { active: browseMode === 'songs' }]">
                <tags-by-user-query-remove
                  :current-mode="browseMode"
                  api-type="songs"
                />
              </div>
              <div :class="['tab-pane', { active: browseMode === 'artists' }]">
                <tags-by-user-query-remove
                  :current-mode="browseMode"
                  api-type="artists"
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

import VueClipboard from "vue-clipboard2";
import { getClientType } from "@/utils";
import TagsByUserQueryRemove from "@/components/pages/TagsByUserQueryRemove.vue";
import { ClientType } from "@/backend/dto/enumeration";
import { BrowseMode } from "@/pages/console/utils";

Vue.use(VueClipboard);

@Component({
  components: {
    TagsByUserQueryRemove,
    NavBarMenu
  }
})
export default class extends Vue {
  @Prop()
  private browseMode!: BrowseMode;

  private clientType: ClientType = getClientType();
}
</script>
