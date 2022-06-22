<template>
  <div>
    <nav-bar-menu active-mode="events" />
    <div style="display: flex; align-items: center">
      <b-container class="col-lg-11">
        <b-toaster class="b-toaster-top-center" name="toaster-2"></b-toaster>
        <b-toast
          id="error"
          title="Error"
          no-auto-hide
          variant="danger"
          class="m-0 rounded-0"
          toaster="toaster-2"
        >
          <span v-if="alertCode !== 401">
            {{ alertMessage }}
          </span>
          <span v-else>
            Access token has expired.
            <b-link to="login" target="_blank">
              Relogin
              <font-awesome-icon class="ml-0" icon="fas fa-external-link" />
            </b-link>
            and try again
          </span>
        </b-toast>
        <b-tabs v-model="browseMode" class="mt-3" content-class="mt-3">
          <b-tab title="Browse by event tag (VocaDB)">
            <event-by-voca-db-tag-tab :mode="browseMode" />
          </b-tab>
        </b-tabs>
      </b-container>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import {
  DateComparisonResult,
  MinimalTag,
  ReleaseEventForApiContractSimplified,
  ReleaseEventForDisplay,
  SongForApiContractSimplifiedWithReleaseEvent
} from "@/backend/dto";
import { api } from "@/backend";

import { DateTime } from "luxon";
import VueClipboard from "vue-clipboard2";
import NavBarMenu from "@/components/NavBarMenu.vue";
import EventByVocaDbTagTab from "@/components/pages/EventByVocaDbTagTab.vue";

Vue.use(VueClipboard);

@Component({ components: { NavBarMenu, EventByVocaDbTagTab } })
export default class extends Vue {
  private alertMessage: string = "";
  private alertCode: number = 0;
  private browseMode = 0;
}
</script>
