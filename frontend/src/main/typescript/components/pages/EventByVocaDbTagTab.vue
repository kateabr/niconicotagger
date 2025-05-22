<template>
  <div>
    <error-message
      :alert-status-text="alertStatusText"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <b-row>
      <b-col></b-col>
      <b-col cols="5" class="m-auto">
        <b-input-group inline class="mt-lg-3">
          <template #prepend>
            <b-button
              v-b-toggle="'scope-collapse-' + thisMode"
              variant="primary"
              style="width: 80px"
              :disabled="defaultDisableCondition() || !event.valid"
              ><font-awesome-icon
                class="mr-sm-1"
                icon="fas fa-angle-down"
              />More</b-button
            >
          </template>
          <b-form-input
            id="tag-form"
            v-model="eventName"
            :disabled="defaultDisableCondition()"
            placeholder="Event name"
            @keydown.enter.native="loadInitialPage()"
          >
          </b-form-input>
          <template #append>
            <b-button
              v-if="!fetching"
              variant="primary"
              style="width: 80px"
              :disabled="eventName === '' || defaultDisableCondition()"
              @click="fetchEvent()"
              >Load</b-button
            >
            <b-button v-else variant="primary" style="width: 80px" disabled
              ><b-spinner small></b-spinner
            ></b-button>
          </template>
        </b-input-group>
        <b-collapse
          :id="'scope-collapse-' + thisMode"
          v-model="showCollapse"
          class="mt-2"
        >
          <b-row>
            <b-col>
              <template>
                <b-input-group
                  inline
                  :state="pageStateIsValid"
                  invalid-feedback="Wrong page number"
                >
                  <template #prepend>
                    <b-input-group-text
                      class="justify-content-center"
                      style="width: 80px"
                      >Page:
                    </b-input-group-text>
                  </template>
                  <template>
                    <b-form-input
                      id="page-jump-form"
                      v-model.number="pageToJump"
                      type="number"
                      :disabled="defaultDisableCondition()"
                      aria-describedby="input-live-help input-live-feedback"
                      :state="pageStateIsValid()"
                      @keydown.enter.native="
                        pageStateIsValid() ? loadPage(pageToJump) : null
                      "
                    >
                    </b-form-input>
                  </template>
                  <template #append>
                    <b-button
                      style="width: 80px"
                      :variant="pageStateIsValid() ? 'success' : 'danger'"
                      :disabled="
                        defaultDisableCondition() || !pageStateIsValid()
                      "
                      @click="loadPage(pageToJump)"
                      ><span v-if="pageToJump === page">Refresh</span
                      ><span v-else>Jump</span></b-button
                    >
                  </template>
                </b-input-group>
              </template>
            </b-col>
          </b-row>
        </b-collapse>
      </b-col>
      <b-col class="m-auto text-left mt-lg-3 ml-n1">
        <b-button
          v-if="eventInfoLoaded()"
          variant="link"
          :disabled="defaultDisableCondition()"
          @click="loadPage(page)"
        >
          <font-awesome-icon icon="fa-solid fa-arrow-rotate-right" />
        </b-button>
      </b-col>
    </b-row>
    <Transition appear>
      <b-row v-if="tempTags.length > 0 && isActiveMode()" class="mt-5">
        <b-col cols="3"></b-col>
        <b-col>
          <b-card>
            <b-card-header>
              Following tags are currently associated with
              <b-link :href="getVocaDBEventUrl(event.id)" target="_blank"
                >{{ event.name }}
              </b-link>
              <span v-if="event.dateString != null"
                >({{ event.dateString }})</span
              ><span v-else class="text-danger"
                >(event dates not specified)</span
              >:
            </b-card-header>
            <b-card-body>
              <b-button-group>
                <span
                  v-for="tempTag in tempTags"
                  :key="tempTag.id"
                  class="mx-1"
                >
                  <b-button
                    :pressed="tempTag.id == eventTag.id"
                    variant="outline-dark"
                    @click="setEventTag(tempTag)"
                  >
                    <font-awesome-icon icon="fas fa-tag" class="mr-1" />
                    {{ tempTag.name }}
                  </b-button>
                </span>
              </b-button-group>
              <div v-if="eventTag.id < 0" class="small text-danger mt-2">
                Please select one
              </div>
            </b-card-body>
            <b-card-footer>
              <b-row class="flex-fill mt-3">
                <b-col cols="6">
                  <b-button
                    :disabled="defaultDisableCondition() || eventTag.id < 0"
                    block
                    variant="success"
                    @click="confirmAndLoad()"
                  >
                    <font-awesome-icon icon="fa-solid fa-check" class="mr-1" />
                    Continue
                  </b-button>
                </b-col>
                <b-col>
                  <b-button
                    :disabled="defaultDisableCondition()"
                    block
                    @click="fetchEvent()"
                  >
                    <font-awesome-icon
                      icon="fa-solid fa-arrow-rotate-right"
                      class="mr-1"
                    />
                    Reload
                  </b-button>
                </b-col>
              </b-row>
            </b-card-footer>
          </b-card>
        </b-col>
        <b-col cols="3"></b-col>
      </b-row>
    </Transition>
    <b-row v-if="eventInfoLoaded() && isActiveMode()">
      <b-row
        class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
      >
        <b-col>
          <b-row>
            <b-col>
              {{ event.category }}:<br />
              <strong>
                <b-link :href="getVocaDBEventUrl(event.id)" target="_blank"
                  >{{ event.name }}
                </b-link>
              </strong>
            </b-col>
            <b-col>
              Held on:<br />
              <strong>
                <font-awesome-icon icon="fa-solid fa-calendar" class="mr-1" />{{
                  event.dateString
                }}</strong
              >
            </b-col>
            <b-col
              >Tag to replace:
              <div>
                <strong
                  ><b-link :href="getVocaDBTagUrl(eventTag.id)" target="_blank"
                    >{{ eventTag.name }}
                  </b-link></strong
                >
              </div></b-col
            >
            <b-col>
              Songs found:<br />
              <strong>{{ totalEntryCount }}</strong>
            </b-col>
            <b-col class="my-auto">
              <b-dropdown
                :disabled="defaultDisableCondition() || !isActiveMode()"
                block
                :text="getMaxResultsForDisplay()"
                class="my-auto"
                variant="primary"
                menu-class="w-100"
              >
                <span
                  v-for="maxResultOption in maxResultsOptions"
                  :key="maxResultOption"
                >
                  <b-dropdown-item
                    :disabled="maxResults === maxResultOption"
                    @click="setMaxResults(maxResultOption)"
                    >{{ maxResultOption }}
                  </b-dropdown-item>
                </span>
              </b-dropdown>
            </b-col>
            <b-col class="my-auto">
              <b-dropdown
                block
                :disabled="defaultDisableCondition() || !isActiveMode()"
                :text="getOrderingConditionForDisplay()"
                variant="primary"
                menu-class="w-100"
              >
                <b-dropdown-item
                  v-for="(key, value) in orderOptions"
                  :key="key"
                  :disabled="orderingCondition === value"
                  @click="setOrderingCondition(value)"
                >
                  {{ orderOptions[value] }}
                </b-dropdown-item>
              </b-dropdown>
            </b-col>
          </b-row>
        </b-col>
      </b-row>
      <b-row v-if="eventInfoLoaded()" class="col-12">
        <b-col class="my-auto">
          <div class="text-center pt-sm-3">
            <b-button-group>
              <b-button
                v-for="(songType, songTypeKey) in songTypeStats"
                :key="songTypeKey"
                class="pl-4 pr-4"
                :disabled="defaultDisableCondition()"
                :variant="
                  (songType.show ? '' : 'outline-') +
                  getSongTypeColorForDisplay(songType.type)
                "
                @click="
                  songType.show = !songType.show;
                  filterEntries();
                "
                >{{ songType.type }} ({{ songType.count }})
              </b-button>
            </b-button-group>
          </div>
        </b-col>
      </b-row>
      <b-row v-if="eventInfoLoaded" class="col-12 m-auto">
        <template>
          <div class="overflow-auto m-auto mt-lg-3">
            <b-pagination
              v-model="page"
              align="center"
              :total-rows="totalEntryCount"
              :per-page="maxResults"
              use-router
              first-number
              last-number
              limit="10"
              :disabled="defaultDisableCondition()"
              @change="loadPage"
            />
          </div>
        </template>
      </b-row>
      <b-table-simple
        v-if="eventInfoLoaded() && isActiveMode()"
        hover
        class="mt-1 col-lg-12"
      >
        <b-thead>
          <b-th>
            <b-form-checkbox
              v-model="allChecked"
              :disabled="defaultDisableCondition()"
              size="lg"
              @change="toggleCheckAll"
            />
          </b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-2 align-middle">Current release event</b-th>
          <b-th class="col-3 align-middle">Release date</b-th>
          <b-th class="col-3 align-middle">Proposed actions</b-th>
          <b-th class="col-1"></b-th>
        </b-thead>
        <b-tbody v-if="!allInvisible()">
          <tr v-for="entry in entries.filter(e => e.visible)" :key="entry.id">
            <td>
              <b-form-checkbox
                v-if="!entry.processed"
                v-model="entry.checked"
                :disabled="defaultDisableCondition()"
                size="lg"
              />
            </td>
            <td>
              <b-link target="_blank" :href="getVocaDBEntryUrl(entry.id)"
                >{{ entry.name
                }}<b-badge
                  class="badge text-center ml-2"
                  :variant="getSongTypeColorForDisplay(entry.type)"
                >
                  {{ getShortenedSongType(entry.type) }}
                </b-badge></b-link
              >
              <div class="text-muted">
                {{ entry.artistString }}
              </div>
            </td>
            <td>
              <span v-if="entry.events.length > 0">
                <div
                  v-for="event in entry.events"
                  :key="event.id"
                  class="font-weight-bolder"
                >
                  <b-badge
                    class="badge text-center"
                    :variant="getEventColorVariant(event)"
                    :href="getVocaDBEventUrl(event.id)"
                    target="_blank"
                  >
                    {{ event.name }}
                  </b-badge>
                </div>
              </span>
              <span v-else class="text-muted">Unspecified</span>
            </td>
            <td>
              <date-disposition
                :disposition="entry.disposition"
                :publish-date="entry.publishedOn"
              />
            </td>
            <td>
              <action
                v-if="!entry.processed"
                :event="entry.toAddEvent ? event : null"
                :tag-to-remove="eventTag"
                :client-type="clientType"
              />
              <entry-error-report :error-report="entry.errorReport" />
            </td>
            <td class="text-center">
              <b-button
                v-if="!entry.visible"
                disabled
                class="btn"
                variant="success"
              >
                <font-awesome-icon icon="fas fa-check" />
              </b-button>
              <b-button
                v-else
                :disabled="defaultDisableCondition()"
                class="btn"
                variant="outline-success"
                @click="processSingle(entry)"
              >
                <font-awesome-icon icon="fas fa-play" />
              </b-button>
            </td>
          </tr>
        </b-tbody>
        <b-tbody v-else>
          <b-tr>
            <b-td colspan="6" class="text-center text-muted">
              <small>No items to display</small>
            </b-td>
          </b-tr>
        </b-tbody>
        <b-tfoot>
          <b-th
            ><b-form-checkbox
              v-model="allChecked"
              size="lg"
              :disabled="defaultDisableCondition()"
              @change="toggleCheckAll"
          /></b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-2 align-middle">Current release event</b-th>
          <b-th class="col-3 align-middle">Release date</b-th>
          <b-th class="col-3 align-middle">Proposed actions</b-th>
          <b-th class="col-1"></b-th>
        </b-tfoot>
      </b-table-simple>
      <b-row
        v-if="eventInfoLoaded()"
        class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
      >
        <b-col class="col-lg-3 m-auto">
          <b-button
            block
            variant="primary"
            :disabled="countChecked() === 0 || massAssigning || fetching"
            @click="processMultiple"
          >
            <div v-if="massAssigning">
              <b-spinner small class="mr-1"></b-spinner>
              Processing...
            </div>
            <div v-else>Batch process ({{ countChecked() }} selected)</div>
          </b-button>
        </b-col>
      </b-row>

      <b-row v-if="eventInfoLoaded()" class="col-12 m-auto">
        <template>
          <div class="overflow-auto m-auto my-lg-3">
            <b-pagination
              v-model="page"
              align="center"
              :total-rows="totalEntryCount"
              :per-page="maxResults"
              use-router
              first-number
              last-number
              limit="10"
              :disabled="defaultDisableCondition()"
              @change="loadPage"
            />
          </div>
        </template>
      </b-row>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { api } from "@/backend";
import Component from "vue-class-component";
import { Prop } from "vue-property-decorator";
import {
  formatDateString,
  getClientType,
  getErrorData,
  getEventColorVariant,
  getMaxResultsForDisplay,
  getOrderingConditionForDisplay,
  getShortenedSongType,
  getSongTypeColorForDisplay,
  getUniqueElementId,
  getVocaDBEventUrl,
  getVocaDBSongUrl,
  getVocaDBTagUrl,
  mapSongTypeStats,
  pageStateIsValid
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import DateDisposition from "@/components/DateDisposition.vue";
import { AxiosError, AxiosResponse } from "axios";
import Action from "@/components/Action.vue";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import { ClientType, DbSortOrder, SongType } from "@/backend/dto/enumeration";
import { SongTypeStatsRecord } from "@/backend/dto/songTypeStats";
import { ReleaseEvent, VocaDbTag } from "@/backend/dto/lowerLevelStruct";
import { SongTagsAndEventsMassUpdateRequest } from "@/backend/dto/request/songTagsAndEventsUpdateRequest";
import { ReleaseEventData, ReleaseEventDataWithVocaDbTag } from "@/backend/dto/higherLevelStruct";
import { SongEntryByVocaDbTagForEvent } from "@/backend/dto/response/songsByVocaDbEventTagResponse";
import {
  localStorageKeyDbOrderBy,
  localStorageKeyEventByDbTagName,
  localStorageKeyMaxResults,
  maxResultsOptions,
  vocaDbOrderOptions
} from "@/constants";

@Component({
  components: { EntryErrorReport, Action, ErrorMessage, DateDisposition }
})
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly targName: string | undefined;

  private readonly maxResultsOptions: number[] = maxResultsOptions;

  // main variables
  private event: ReleaseEventData = {
    id: -1,
    name: "",
    category: "Unspecified",
    date: null,
    endDate: null,
    dateString: null,
    valid: false
  };
  private entries: SongEntryByVocaDbTagForEvent[] = [];
  private tempTags: VocaDbTag[] = [];
  private eventTag: VocaDbTag = { id: -1, name: "" };
  private eventName: string = "";
  private songTypeStats: SongTypeStatsRecord[] = [];

  // api variables
  private pageToJump: number = 1;
  private maxResults: number = 10;
  private orderingCondition: DbSortOrder = "PublishDate";
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;
  private clientType: ClientType = getClientType();

  // interface variables
  private allChecked: boolean = false;
  private showCollapse: boolean = false;
  private totalEntryCount: number = 0;
  private page: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 0;

  // error handling
  private alertStatusText = "";
  private alertMessage: string = "";

  // interface dictionaries
  private readonly orderOptions = vocaDbOrderOptions;

  // proxy methods
  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(SongType[songType]);
  }

  private getVocaDBEventUrl(id: number): string {
    return getVocaDBEventUrl(this.clientType, id);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBSongUrl(this.clientType, id);
  }

  private getVocaDBTagUrl(tag: VocaDbTag): string {
    return getVocaDBTagUrl(this.clientType, tag.id);
  }

  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getOrderingConditionForDisplay(): string {
    return getOrderingConditionForDisplay(this.orderingCondition);
  }

  private getSongTypeColorForDisplay(typeString: SongType): string {
    return getSongTypeColorForDisplay(typeString);
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private eventInfoLoaded(): boolean {
    return this.event.valid && this.numOfPages > 0;
  }

  private allInvisible(): boolean {
    return !this.entries.some(entry => entry.visible);
  }

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning || this.assigning;
  }

  private getHiddenTypes(): number {
    return this.songTypeStats.filter(statsItem => !statsItem.show).length;
  }

  private toggleCheckAll(): void {
    for (const item of this.entries) {
      if (!item.visible || item.processed) continue;
      item.checked = this.allChecked;
    }
  }

  private countChecked(): number {
    return this.entries.filter(item => item.checked).length;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
    localStorage.setItem(localStorageKeyMaxResults, this.maxResults.toString());
  }

  private setOrderingCondition(value: DbSortOrder): void {
    this.orderingCondition = value;
    localStorage.setItem(localStorageKeyDbOrderBy, this.orderingCondition);
  }

  private getEventColorVariant(event: ReleaseEvent): string {
    return getEventColorVariant(event, this.event.id);
  }

  private setEventTag(tempTag: VocaDbTag) {
    this.eventTag = tempTag;
  }

  // row filtering
  private hiddenTypeFlag(entry: SongEntryByVocaDbTagForEvent): boolean {
    return (
      this.getHiddenTypes() == 0 ||
      !this.songTypeStats
        .filter(statsItem => !statsItem.show)
        .map(statsItem => statsItem.type)
        .includes(entry.type)
    );
  }

  private filterEntries(): void {
    for (const entry of this.entries) {
      entry.visible =
        !entry.processed &&
        (entry.errorReport != null || this.hiddenTypeFlag(entry));
    }
  }

  private updateUrl(): void {
    this.$router
      .push({
        name: "events-full",
        params: { browseMode: this.thisMode, targName: this.eventName }
      })
      .catch(err => {
        return false;
      });
  }

  // error handling
  private processError(response: AxiosResponse | undefined): void {
    const errorData = getErrorData(response);
    this.alertMessage = errorData.message;
    this.alertStatusText = errorData.statusText;
    this.$bvToast.show(getUniqueElementId("error_", this.thisMode.toString()));
  }

  // api methods
  async fetchEvent(): Promise<void> {
    this.updateUrl();
    this.showCollapse = false;
    this.entries = [];
    this.fetching = true;
    this.eventTag = { id: -1, name: "" };
    this.event.valid = false;
    try {
      let response = await api.getReleaseEventWithLinkedTag({
        eventName: this.eventName,
        clientType: this.clientType
      });
      this.event = {
        id: response.id,
        name: response.name,
        category: response.category,
        dateString: formatDateString(response.date, response.endDate),
        date: response.date,
        endDate: response.endDate,
        valid: false
      };
      this.tempTags = response.vocaDbTags;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      localStorage.setItem(localStorageKeyEventByDbTagName, this.eventName);
      this.fetching = false;
    }
  }

  async fetchSongs(newStartOffset: number, newPage: number): Promise<void> {
    this.showCollapse = false;
    this.fetching = true;
    try {
      this.pageToJump = newPage;
      let response = await api.getVocaDbSongEntriesByVocaDbTagId({
        tagId: this.eventTag.id,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.orderingCondition,
        dates: {
          from: this.event.date,
          to: this.event.endDate,
          applyToSearch: false
        },
        clientType: this.clientType
      });
      this.entries = response.items.map(item => {
        return {
          id: item.id,
          name: item.name,
          type: item.type,
          artistString: item.artistString,
          publishedOn: new Date(item.publishedOn).toLocaleDateString(),
          events: item.events,
          toAddEvent: !item.events.some(
            itemEvent => itemEvent.id == this.event.id
          ),
          processed: false,
          disposition: item.disposition,
          checked: false,
          visible: true,
          errorReport: null
        };
      });
      this.songTypeStats = mapSongTypeStats(
        response.songTypeStats,
        this.songTypeStats
      );
      this.filterEntries();
      this.totalEntryCount = response.totalCount;
      this.numOfPages = this.totalEntryCount / this.maxResults + 1;
      this.allChecked = false;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      localStorage.setItem(localStorageKeyEventByDbTagName, this.eventName);
      localStorage.setItem(
        localStorageKeyMaxResults,
        this.maxResults.toString()
      );
      localStorage.setItem(localStorageKeyDbOrderBy, this.orderingCondition);
      this.maxPage = Math.ceil(this.totalEntryCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  private async processSingle(
    song: SongEntryByVocaDbTagForEvent
  ): Promise<void> {
    this.assigning = true;
    try {
      await this.update([song]);
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.assigning = false;
    }
  }

  private async processMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      await this.update(this.entries.filter(entry => entry.checked));
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
    }
  }

  private async update(
    entriesToUpdate: SongEntryByVocaDbTagForEvent[]
  ): Promise<void> {
    const errors = await api.updateSongEventsAndTags(
      this.buildRequest(entriesToUpdate)
    );
    const entriesWithErrors = errors.map(error => error.entryId);
    const eventToAdd = {
      id: this.event.id,
      name: this.event.name
    };
    for (const entry of entriesToUpdate) {
      if (!entriesWithErrors.includes(entry.id)) {
        if (entry.toAddEvent) {
          entry.events.push(eventToAdd);
          entry.toAddEvent = false;
        }
        entry.processed = true;
        entry.checked = false;
        entry.errorReport = null;
      } else {
        entry.errorReport = errors.filter(
          error => error.entryId == entry.id
        )[0];
      }
    }
  }

  private buildRequest(
    entries: SongEntryByVocaDbTagForEvent[]
  ): SongTagsAndEventsMassUpdateRequest {
    const eventToAdd = {
      id: this.event.id,
      name: this.event.name
    };
    return {
      subRequests: entries.map(entry => {
        return {
          entryId: entry.id,
          tags: [this.eventTag],
          event: entry.toAddEvent ? eventToAdd : null
        };
      }),
      clientType: this.clientType
    };
  }

  private loadInitialPage(): void {
    this.updateUrl();
    this.fetchSongs(0, 1);
  }

  private confirmAndLoad(): void {
    this.event.valid = true;
    this.tempTags = [];
    this.numOfPages = 0;
    this.fetchSongs(0, 1);
  }

  private loadPage(page: number): void {
    this.updateUrl();
    this.fetchSongs((page - 1) * this.maxResults, page);
  }

  // session
  created(): void {
    let max_results = localStorage.getItem(localStorageKeyMaxResults);
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
    let order_by = localStorage.getItem(
      localStorageKeyDbOrderBy
    ) as DbSortOrder;
    if (order_by != null) {
      this.orderingCondition = order_by;
    }
    if (this.targName != undefined) {
      this.eventName = this.targName;
    }
    let event_tag_name = localStorage.getItem(localStorageKeyEventByDbTagName);
    if (event_tag_name != null) {
      this.eventName = event_tag_name;
    }
  }

  // fill event tag name from address params (override local storage)
  mounted(): void {
    let targName = this.$route.params["targName"];
    if (targName != undefined) {
      this.eventName = targName;
    }
  }
}
</script>
