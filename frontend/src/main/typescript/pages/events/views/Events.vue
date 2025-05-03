<template>
  <div>
    <nav-bar-menu active-mode="events" :client-type="clientType" />
    <b-toaster class="b-toaster-top-full" name="toaster-events" />
    <div style="display: flex; align-items: center">
      <b-container class="col-lg-11">
        <b-nav tabs class="mb-2">
          <b-nav-item
            :to="{ name: 'events-mode', params: { browseMode: 'nicovideo' } }"
            :active="browseMode === 'nicovideo'"
          >
            Add event by associated tags (NND)
          </b-nav-item>
          <b-nav-item
            :to="{ name: 'events-mode', params: { browseMode: 'vocadb' } }"
            :active="browseMode === 'vocadb'"
          >
            Replace an event tag (VocaDB)
          </b-nav-item>
        </b-nav>
        <div class="tab-content">
          <div :class="['tab-pane', { active: browseMode === 'nicovideo' }]">
            <event-by-nnd-tag-tab
              :mode="browseMode"
              this-mode="nicovideo"
              :targ-name="targName"
            />
          </div>
          <div :class="['tab-pane', { active: browseMode === 'vocadb' }]">
            <event-by-voca-db-tag-tab :mode="browseMode" this-mode="vocadb" />
          </div>
        </div>
      </b-container>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";
import VueClipboard from "vue-clipboard2";
import NavBarMenu from "@/components/NavBarMenu.vue";
import EventByVocaDbTagTab from "@/components/pages/EventByVocaDbTagTab.vue";
import EventByNndTagTab from "@/components/pages/EventByNndTagTab.vue";
import { getClientType } from "@/utils";
import { ClientType } from "@/backend/dto/enumeration";

Vue.use(VueClipboard);

@Component({
  components: { NavBarMenu, EventByVocaDbTagTab, EventByNndTagTab }
})
export default class extends Vue {
  @Prop()
  private readonly browseMode!: "vocadb" | "nicovideo";

  @Prop()
  private readonly targName: string | undefined;

  private clientType: ClientType = ClientType.UNKNOWN;

  created(): void {
    this.clientType = getClientType();
  }
}
</script>
