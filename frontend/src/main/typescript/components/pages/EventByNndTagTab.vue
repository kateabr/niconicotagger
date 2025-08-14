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
              style="width: 82px"
              :disabled="
                defaultDisableCondition() || event.id < 0 || !tagsLoaded
              "
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
            @keydown.enter.native="fetchEvent(eventName)"
          >
          </b-form-input>
          <template #append>
            <b-button
              v-if="!fetching"
              variant="primary"
              style="width: 82px"
              :disabled="eventName === '' || defaultDisableCondition()"
              @click="fetchEvent(eventName)"
              >Load
            </b-button>
            <b-button
              v-if="fetching"
              variant="primary"
              style="width: 82px"
              disabled
            >
              <b-spinner small></b-spinner>
            </b-button>
          </template>
        </b-input-group>
        <b-collapse
          :id="'scope-collapse-' + thisMode"
          v-model="showCollapse"
          class="mt-2"
        >
          <b-row>
            <b-col>
              <b-input-group inline>
                <b-form-input
                  id="scope-tag-form"
                  v-model="scopeTagString"
                  placeholder="Specify scope (NND)"
                  :disabled="defaultDisableCondition()"
                  @keydown.enter.native="loadPage(1)"
                >
                </b-form-input>
                <template #prepend>
                  <b-button
                    variant="secondary"
                    style="width: 82px"
                    @click="setDefaultScopeTagString"
                  >
                    <font-awesome-icon icon="fa-solid fa-paste" />
                  </b-button>
                </template>
                <template #append>
                  <b-button
                    variant="danger"
                    style="width: 82px"
                    :disabled="scopeTagString === ''"
                    @click="scopeTagString = ''"
                    >Clear
                  </b-button>
                </template>
              </b-input-group>
            </b-col>
          </b-row>
          <b-row
            v-if="event.valid && isActiveMode() && tagsConfirmed"
            class="mt-2"
          >
            <b-col>
              <b-dropdown
                block
                :disabled="defaultDisableCondition()"
                :text="getSortingConditionForDisplay()"
                variant="primary"
                menu-class="w-100"
              >
                <b-dropdown-item
                  v-for="(key, item) in sortingOptions"
                  :key="key"
                  :disabled="sortingCondition === item"
                  @click="setSortingCondition(item)"
                >
                  {{ sortingOptions[item] }}
                </b-dropdown-item>
              </b-dropdown>
            </b-col>
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
                      style="width: 82px"
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
                      style="width: 82px"
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
          <b-row
            v-if="event.valid && isActiveMode() && tagsConfirmed"
            class="mt-2"
          >
            <b-col>
              <b-form-checkbox
                v-model="showVideosWithUploaderEntry"
                @change="filterVideos()"
              >
                Force show videos whose uploaders have entries at VocaDB
              </b-form-checkbox>
              <b-form-checkbox
                v-model="hideSongsWithSameSeriesEvents"
                @change="filterVideos()"
              >
                Hide songs with release events from the same series
              </b-form-checkbox>
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
      <b-row v-if="tagsLoaded && isActiveMode() && !tagsConfirmed" class="mt-5">
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
              <div v-for="(tag, key) in event.nndTags" :key="key">
                <b-link :href="getNicoTagUrl(tag)" target="_blank">
                  <font-awesome-icon icon="fas fa-tag" class="mr-1" />
                  {{ tag }}
                </b-link>
              </div>
            </b-card-body>
            <b-card-footer>
              <div class="mb-3 text-secondary">
                <b-form-checkbox v-model="filterByEventDates"
                  >Search only within the event date boundaries
                  <div class="small">
                    (helps to reduce clutter in the output when there are NND
                    tags linked directly to event series)
                  </div>
                </b-form-checkbox>
              </div>
              <span>All good?</span>
              <b-row class="flex-fill mt-3">
                <b-col cols="6">
                  <b-button
                    :disabled="defaultDisableCondition()"
                    block
                    variant="success"
                    @click="confirmAndLoad()"
                  >
                    <font-awesome-icon icon="fa-solid fa-check" class="mr-1" />
                    Yes, continue
                  </b-button>
                </b-col>
                <b-col>
                  <b-button
                    :disabled="defaultDisableCondition()"
                    block
                    @click="fetchEvent(eventName)"
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
            <b-col>
              Videos found:<br />
              <strong
                >{{ totalVideoCount }}
                <b-icon
                  v-if="!filterByEventDates"
                  icon="funnel"
                  scale="1.20"
                ></b-icon>
                <b-icon v-else icon="funnel-fill" scale="1.20"></b-icon>
                <b-iconstack v-if="scopeTagStringFrozen != ''" class="ml-1">
                  <b-icon stacked icon="circle" scale="1.20"></b-icon>
                  <b-icon
                    stacked
                    icon="circle-fill"
                    scale="0.8"
                  ></b-icon></b-iconstack
                ><b-icon
                  v-else
                  class="ml-1"
                  icon="circle-fill"
                  scale="1.20"
                ></b-icon
              ></strong>
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
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="currentEventFilled"
                @click="filterVideos()"
                >Songs with current event
              </b-button>
            </b-col>
            <b-col class="my-auto">
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="showVideosWithoutEntries"
                @click="filterVideos()"
                >Videos without entries
              </b-button>
            </b-col>
          </b-row>
        </b-col>
      </b-row>
      <b-row v-if="eventInfoLoaded()" class="col-12 m-auto">
        <template>
          <div class="overflow-auto m-auto mt-lg-3">
            <b-pagination
              v-model="page"
              align="center"
              :total-rows="totalVideoCount"
              :per-page="maxResults"
              use-router
              first-number
              last-number
              limit="10"
              :disabled="defaultDisableCondition()"
              @input="loadPage(page)"
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
          <b-th class="col-3 align-middle">Video</b-th>
          <b-th class="col-2 align-middle">Current release events</b-th>
          <b-th class="col-3 align-middle">Release date</b-th>
          <b-th class="col-3 align-middle">Proposed actions</b-th>
          <b-th class="col-1"></b-th>
        </b-thead>
        <b-tbody v-if="!allInvisible()">
          <tr
            v-for="item in entries.filter(item1 => item1.visible)"
            :key="item.video.id"
          >
            <td>
              <b-form-checkbox
                v-if="isSelectable(item)"
                v-model="item.toUpdate"
                :disabled="defaultDisableCondition()"
                size="lg"
              />
            </td>
            <td>
              <b-button
                :disabled="defaultDisableCondition()"
                size="sm"
                variant="primary-outline"
                class="mr-2"
                @click="item.video.visible = !item.video.visible"
              >
                <font-awesome-icon icon="fas fa-play" />
              </b-button>
              <b-link target="_blank" :href="getNicoVideoUrl(item.video.id)"
                >{{ item.video.title }}
              </b-link>
              <b-badge
                v-clipboard:copy="item.video.length"
                variant="primary"
                class="m-sm-1"
                href="#"
                >{{ item.video.length }}</b-badge
              >
              <div>
                <b-badge
                  v-for="tag in item.video.tags"
                  :key="tag.name"
                  v-clipboard:copy="tag.name"
                  class="m-sm-1"
                  :variant="tag.type"
                  href="#"
                >
                  <font-awesome-icon icon="fas fa-tag" class="mr-1" />
                  {{ tag.name }}
                </b-badge>
              </div>
              <b-collapse
                :visible="item.video.visible && !fetching"
                class="mt-2 collapsed"
              >
                <nico-embed
                  v-if="item.video.visible && !fetching"
                  :content-id="item.video.id"
                />
              </b-collapse>
              <nico-description
                :content-id="item.video.id"
                :description="item.video.description"
                :publisher="item.publisher"
              />
            </td>
            <td>
              <span v-if="item.entry != null && item.entry.events.length > 0">
                <div
                  v-for="(releaseEvent, key1) in item.entry.events"
                  :key="key1"
                  class="font-weight-bolder"
                >
                  <b-badge
                    class="badge text-center"
                    :variant="getEventColorVariant(releaseEvent)"
                    :href="getVocaDBEventUrl(releaseEvent.id)"
                    target="_blank"
                  >
                    {{ releaseEvent.name }}
                  </b-badge>
                </div>
              </span>
              <span v-else-if="item.entry != null" class="text-muted"
                >Unspecified</span
              >
              <span v-else class="text-muted">None</span>
            </td>
            <td class="pb-2">
              <div v-if="item.entry != null">
                <b-link target="_blank" :href="getVocaDBEntryUrl(item.entry.id)"
                  >{{ item.entry.name }}
                  <b-badge
                    class="badge text-center ml-2"
                    :variant="getSongTypeColorForDisplay(item.entry.type)"
                  >
                    {{ getShortenedSongType(item.entry.type) }}
                  </b-badge>
                </b-link>
                <div class="text-muted mb-2">
                  {{ item.entry.artistString }}
                </div>
              </div>
              <date-disposition
                :disposition="item.disposition"
                :publish-date="item.publishedOn"
              />
            </td>
            <td>
              <div v-if="item.entry !== null" class="mb-2">
                <action v-if="!item.processed" :event="event" />
              </div>
              <div v-else>
                <add-to-the-database
                  :disabled="fetching"
                  :client-type="clientType"
                  :publisher="item.publisher"
                  :video-id="item.video.id"
                />
              </div>
              <entry-error-report :error-report="item.errorReport" />
            </td>
            <td class="text-center">
              <span v-if="item.entry !== null">
                <b-button
                  v-if="item.processed"
                  disabled
                  class="btn"
                  variant="success"
                >
                  <font-awesome-icon icon="fas fa-check" />
                </b-button>
                <b-button
                  v-else
                  :disabled="defaultDisableCondition() || !isSelectable(item)"
                  class="btn"
                  variant="outline-success"
                  @click="updateSingle(item)"
                >
                  <font-awesome-icon icon="fas fa-play" />
                </b-button>
              </span>
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
          <b-th>
            <b-form-checkbox
              v-model="allChecked"
              size="lg"
              :disabled="defaultDisableCondition()"
              @change="toggleCheckAll"
            />
          </b-th>
          <b-th class="col-3 align-middle">Video</b-th>
          <b-th class="col-2 align-middle">Current release events</b-th>
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
            @click="updateMultiple"
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
              :total-rows="totalVideoCount"
              :per-page="maxResults"
              use-router
              first-number
              last-number
              limit="10"
              :disabled="defaultDisableCondition()"
              @input="loadPage(page)"
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
  allVideosInvisible,
  getErrorData,
  formatDateString,
  getClientType,
  getEventColorVariant,
  getMaxResultsForDisplay,
  getNicoTagUrl,
  getNicoVideoUrl,
  getShortenedSongType,
  getSongTypeColorForDisplay,
  getSortingConditionForDisplayNico,
  getUniqueElementId,
  getVocaDBAddSongUrl,
  getVocaDBEventUrl,
  getVocaDBSongUrl,
  pageStateIsValid
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import NicoEmbed from "@/components/NicoEmbed.vue";
import DateDisposition from "@/components/DateDisposition.vue";
import { AxiosError, AxiosResponse } from "axios";
import Action from "@/components/Action.vue";
import NicoDescription from "@/components/NicoDescription.vue";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import { NndSortOrder, SongType } from "@/backend/dto/enumeration";
import { ReleaseEvent } from "@/backend/dto/lowerLevelStruct";
import { MassAddReleaseEventRequest } from "@/backend/dto/request/addReleaseEventRequest";
import {
  ReleaseEventDataWithNndTags,
  SongEntryWithReleaseEventInfo
} from "@/backend/dto/higherLevelStruct";
import { NndVideoWithAssociatedVocaDbEntryForEvent } from "@/backend/dto/response/videosByTagsResponseForEvent";
import {
  defaultScopeTagString,
  localStorageKeyEventByNndTagsName,
  localStorageKeyMaxResults,
  localStorageKeyNndOrderBy,
  maxResultsOptions,
  nndOrderOptions
} from "@/constants";
import AddToTheDatabase from "@/components/AddToTheDatabase.vue";

@Component({
  methods: {
    getNicoVideoUrl
  },
  components: {
    AddToTheDatabase,
    EntryErrorReport,
    NicoDescription,
    Action,
    ErrorMessage,
    NicoEmbed,
    DateDisposition
  }
})
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly targName: string | undefined;

  private readonly maxResultsOptions: number[] = maxResultsOptions;
  private readonly sortingOptions = nndOrderOptions;

  // main variables
  private event: ReleaseEventDataWithNndTags = {
    id: -1,
    name: "",
    category: "Unspecified",
    dateString: "",
    date: null,
    endDate: null,
    nndTags: [],
    valid: false,
    seriesId: null
  };
  private entries: NndVideoWithAssociatedVocaDbEntryForEvent[] = [];
  private eventName: string = "";
  private scopeTagString: string = "";
  private scopeTagStringFrozen: string = "";

  // api variables
  private pageToJump: number = 1;
  private maxResults: number = 10;
  private sortingCondition: NndSortOrder = "startTime";
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;
  private clientType: string = getClientType();

  // interface variables
  private tagsLoaded: boolean = false;
  private tagsConfirmed: boolean = false;
  private allChecked: boolean = false;
  private showCollapse: boolean = false;
  private totalVideoCount: number = 0;
  private page: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 0;
  private currentEventFilled: boolean = false;
  private showVideosWithoutEntries: boolean = false;
  private showVideosWithUploaderEntry: boolean = true;
  private hideSongsWithSameSeriesEvents: boolean = false;
  private filterByEventDates: boolean = true;

  // error handling
  private alertStatusText = "";
  private alertMessage: string = "";

  // proxy methods
  private getVocaDBEventUrl(id: number): string {
    return getVocaDBEventUrl(this.clientType, id);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBSongUrl(this.clientType, id);
  }

  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getSortingConditionForDisplay(): string {
    return getSortingConditionForDisplayNico(this.sortingCondition);
  }

  private getVocaDBAddSongUrl(contentId: string): string {
    return getVocaDBAddSongUrl(this.clientType, contentId);
  }

  private getNicoTagUrl(tag: string): string {
    return getNicoTagUrl(tag, "");
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private getSongTypeColorForDisplay(typeString: string): string {
    return getSongTypeColorForDisplay(SongType[typeString]);
  }

  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(SongType[songType]);
  }

  private eventInfoLoaded(): boolean {
    return this.tagsConfirmed && this.numOfPages > 0;
  }

  private allInvisible(): boolean {
    return allVideosInvisible(this.entries);
  }

  private setDefaultScopeTagString(): void {
    this.scopeTagString = defaultScopeTagString;
  }

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning || this.assigning;
  }

  private toggleCheckAll(): void {
    for (const item of this.entries) {
      item.toUpdate =
        item.visible && this.isSelectable(item) && this.allChecked;
    }
  }

  private countChecked(): number {
    return this.entries.filter(item => item.toUpdate).length;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
    localStorage.setItem(localStorageKeyMaxResults, this.maxResults.toString());
  }

  private setSortingCondition(value: NndSortOrder): void {
    this.sortingCondition = value;
    localStorage.setItem(localStorageKeyNndOrderBy, value);
  }

  private getEventColorVariant(event: ReleaseEvent): string {
    return getEventColorVariant(event, this.event.id);
  }

  // row filtering
  private currentEventFilledFlag(
    item: NndVideoWithAssociatedVocaDbEntryForEvent
  ): boolean {
    return this.currentEventFilled || item.entry == null || !item.processed;
  }

  private showVideosWithoutEntriesFlag(
    item: NndVideoWithAssociatedVocaDbEntryForEvent
  ): boolean {
    return (
      this.showVideosWithoutEntries ||
      item.entry != null ||
      (this.showVideosWithUploaderEntry &&
        item.publisher != null &&
        item.publisher.type == "DATABASE")
    );
  }

  private showSongsWithSameSeriesEventsFlag(
    item: NndVideoWithAssociatedVocaDbEntryForEvent
  ): boolean {
    return (
      !this.hideSongsWithSameSeriesEvents ||
      item.entry == null ||
      this.event.seriesId == null ||
      item.entry.events.filter(
        event =>
          event.id != this.event.id && event.seriesId == this.event.seriesId
      ).length == 0
    );
  }

  private filterVideos(): void {
    for (const item of this.entries) {
      item.visible =
        this.currentEventFilledFlag(item) &&
        this.showVideosWithoutEntriesFlag(item) &&
        this.showSongsWithSameSeriesEventsFlag(item);
      item.toUpdate = item.toUpdate && item.visible;
    }
  }

  private isSelectable(
    item: NndVideoWithAssociatedVocaDbEntryForEvent
  ): boolean {
    return item.entry != null && !item.processed;
  }

  private updateUrl(eventName: string): void {
    this.$router
      .push({
        name: "events-full",
        params: { browseMode: this.thisMode, targName: eventName }
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
  async fetchEvent(eventName: string): Promise<void> {
    this.updateUrl(eventName);
    this.showCollapse = false;
    this.tagsLoaded = false;
    this.tagsConfirmed = false;
    this.entries = [];
    this.fetching = true;
    try {
      let response = await api.getReleaseEvent({
        eventName: eventName,
        clientType: this.clientType
      });
      this.event = {
        id: response.id,
        name: response.name,
        category: response.category,
        dateString: formatDateString(response.date, response.endDate),
        date: response.date,
        endDate: response.endDate,
        nndTags: response.nndTags,
        seriesId: response.seriesId,
        valid: true
      };
      this.tagsLoaded = true;
      this.filterByEventDates = response.suggestFiltering;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      localStorage.setItem(localStorageKeyEventByNndTagsName, eventName);
      this.fetching = false;
    }
  }

  async fetchVideos(
    scopeString: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    this.showCollapse = false;
    this.fetching = true;
    try {
      let response = await api.getVideosByNndTagsForEvent({
        tags: this.event.nndTags,
        scope: scopeString,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.sortingCondition,
        dates: {
          from: this.event.date,
          to: this.event.endDate,
          applyToSearch: this.filterByEventDates
        },
        eventId: this.event.id,
        clientType: this.clientType
      });
      this.entries = response.items.map(item => {
        return {
          entry: item.entry,
          video: {
            id: item.video.id,
            title: item.video.title,
            description: item.video.description,
            length: item.video.length,
            tags: item.video.tags,
            visible: false
          },
          publishedOn: new Date(item.publishedOn).toLocaleDateString(),
          publisher: item.publisher,
          disposition: item.disposition,
          processed:
            item.entry?.events?.map(e => e.id).includes(this.event.id) ?? false,
          visible: true,
          toUpdate: false,
          errorReport: null
        } as NndVideoWithAssociatedVocaDbEntryForEvent;
      });
      this.filterVideos();
      this.totalVideoCount = response.totalCount;
      this.scopeTagString = response.cleanScope;
      this.scopeTagStringFrozen = response.cleanScope;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.allChecked = false;
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.pageToJump = newPage;
      this.page = newPage;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      localStorage.setItem(localStorageKeyEventByNndTagsName, this.eventName);
      localStorage.setItem(localStorageKeyNndOrderBy, this.sortingCondition);
      localStorage.setItem(
        localStorageKeyMaxResults,
        this.maxResults.toString()
      );
      this.fetching = false;
    }
  }

  private async updateSingle(
    song: NndVideoWithAssociatedVocaDbEntryForEvent
  ): Promise<void> {
    if (song.entry == null) return;

    this.assigning = true;
    try {
      await this.update([song]);
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.assigning = false;
    }
  }

  private async updateMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      await this.update(this.entries.filter(entry => entry.toUpdate));
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
    }
  }

  private async update(
    entries: NndVideoWithAssociatedVocaDbEntryForEvent[]
  ): Promise<void> {
    const errors = await api.addReleaseEvent(
      this.buildRequest(
        entries.map(entry => entry.entry as SongEntryWithReleaseEventInfo)
      )
    );
    const entriesWithErrors = errors.map(error => error.entryId);
    for (const entry of entries) {
      if (!entriesWithErrors.includes(entry.entry?.id as number)) {
        entry.entry?.events.push({
          id: this.event.id,
          name: this.event.name,
          seriesId: null
        });
        entry.processed = true;
        entry.toUpdate = false;
        entry.errorReport = null;
      } else {
        entry.errorReport = errors.filter(
          error => error.entryId == entry.entry?.id
        )[0];
      }
    }
  }

  private buildRequest(
    entries: SongEntryWithReleaseEventInfo[]
  ): MassAddReleaseEventRequest {
    return {
      subRequests: entries.map(entry => {
        return {
          entryId: entry.id,
          event: {
            id: this.event.id,
            name: this.event.name
          }
        };
      }),
      clientType: this.clientType
    };
  }

  private confirmAndLoad(): void {
    this.tagsConfirmed = true;
    this.numOfPages = 0;
    this.loadPage(1);
  }

  private loadPage(page: number): void {
    this.fetchVideos(this.scopeTagString, (page - 1) * this.maxResults, page);
  }

  // session
  created(): void {
    let max_results = localStorage.getItem(localStorageKeyMaxResults);
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
    let event_name = localStorage.getItem(localStorageKeyEventByNndTagsName);
    if (event_name != null) {
      this.eventName = event_name;
    }
    let sort_by = localStorage.getItem(localStorageKeyNndOrderBy);
    if (sort_by != null) {
      this.sortingCondition = sort_by as NndSortOrder;
    }
  }

  // fill event name from address params (override local storage)
  mounted(): void {
    let targName = this.$route.params["targName"];
    if (targName != undefined) {
      this.eventName = targName;
    }
  }
}
</script>

<style lang="scss">
@import "../style.scss";
</style>
