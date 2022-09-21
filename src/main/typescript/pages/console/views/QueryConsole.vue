<template>
  <div>
    <nav-bar-menu active-mode="console" :db-address="dbAddress" />
    <b-row class="col-12 m-0">
      <b-toaster class="b-toaster-top-center" name="toaster-2"></b-toaster>
      <b-col>
        <div style="display: flex; align-items: center">
          <b-container class="col-lg-11">
            <b-nav tabs class="mb-2">
              <b-nav-item
                :to="{ name: 'console-mode', params: { browseMode: 'remove' } }"
                :active="browseMode === 'remove'"
              >
                Remove tags
              </b-nav-item>
            </b-nav>
            <div class="tab-content">
              <div :class="['tab-pane', { active: browseMode === 'remove' }]">
                <tags-by-user-query-remove
                  :mode="browseMode"
                  this-mode="remove"
                  :targ-name="targName"
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
import TagsByUserQueryRemove from "@/components/pages/TagsByUserQueryRemove.vue";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({
  components: {
    NavBarMenu,
    TagsByUserQueryRemove
  }
})
export default class extends Vue {
  @Prop()
  private browseMode!: "remove";

  @Prop()
  private targName!: string;

  private dbAddress: string = "";

  created(): void {
    let dbAddress = localStorage.getItem("dbAddress");
    if (this.dbAddress == "" && dbAddress != null) {
      this.dbAddress = dbAddress;
    }
  }
}
</script>
