<template>
  <div>
    <error-message
      :alert-code="alertCode"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <b-row>
      <span class="m-auto col-lg-5">
        <b-input-group inline class="mt-lg-3">
          <template #prepend>
            <b-button
              v-b-toggle.scope-collapse
              variant="primary"
              style="width: 80px"
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
            v-model.trim="eventName"
            :disabled="defaultDisableCondition() || tagsConfirmed"
            placeholder="Event tag (NND)"
            @keydown.enter.native="fetchEvent(eventName)"
          >
          </b-form-input>
          <template #append>
            <b-button
              v-if="!fetching && !tagsConfirmed"
              variant="primary"
              style="width: 80px"
              :disabled="eventName === '' || defaultDisableCondition()"
              @click="fetchEvent(eventName)"
              >Load</b-button
            >
            <b-button
              v-if="!fetching && tagsConfirmed"
              variant="danger"
              style="width: 80px"
              :disabled="defaultDisableCondition()"
              @click="clear()"
              >Clear</b-button
            >
            <b-button
              v-if="fetching"
              :variant="tagsConfirmed ? 'danger' : 'primary'"
              style="width: 80px"
              disabled
              ><b-spinner small></b-spinner
            ></b-button>
          </template>
        </b-input-group>
        <b-collapse id="scope-collapse" v-model="showCollapse" class="mt-2">
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
                    style="width: 80px"
                    @click="setDefaultScopeTagString"
                  >
                    <font-awesome-icon icon="fa-solid fa-paste" />
                  </b-button>
                </template>
                <template #append>
                  <b-button
                    variant="danger"
                    style="width: 80px"
                    :disabled="scopeTagString === ''"
                    @click="scopeTagString = ''"
                    >Clear</b-button
                  >
                </template>
              </b-input-group>
            </b-col>
          </b-row>
          <b-row v-if="event.name !== '' && isActiveMode()" class="mt-2">
            <b-col>
              <b-dropdown
                block
                :disabled="defaultDisableCondition()"
                :text="getSortingConditionForDisplay()"
                variant="primary"
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
                      style="width: 80px"
                      >Page:</b-input-group-text
                    >
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
          <b-row v-if="event.name !== '' && isActiveMode()" class="mt-2">
            <b-col>
              <b-form-checkbox
                v-model="showVideosWithUploaderEntry"
                @change="filterVideos()"
              >
                Force show videos whose uploaders have entries at VocaDB
              </b-form-checkbox>
            </b-col>
          </b-row>
        </b-collapse>
      </span>
    </b-row>
    <b-row v-if="tagsLoaded && isActiveMode() && !tagsConfirmed" class="mt-5">
      <b-col cols="3"></b-col>
      <b-col>
        <b-card>
          <b-card-header>
            Following tags are currently associated with
            <b-link
              :href="getVocaDBEventUrl(event.id, event.urlSlug)"
              target="_blank"
              >{{ event.name }}</b-link
            >:
          </b-card-header>
          <b-card-body>
            <span v-for="(tag, key) in event.nndTags" :key="key">
              <b-link :href="getNicoTagUrl(tag)" target="_blank"
                ><font-awesome-icon icon="fas fa-tag" class="mr-1" />{{
                  tag
                }}</b-link
              >
            </span>
          </b-card-body>
          <b-card-footer>
            All good?
            <b-row class="flex-fill mt-3">
              <b-col cols="6">
                <b-button
                  :disabled="defaultDisableCondition()"
                  block
                  variant="success"
                  @click="confirmAndLoad()"
                  ><font-awesome-icon
                    icon="fa-solid fa-check"
                    class="mr-1"
                  />Yes, continue</b-button
                >
              </b-col>
              <b-col cols="6">
                <b-button
                  :disabled="defaultDisableCondition()"
                  block
                  @click="fetchEvent(eventName)"
                  ><font-awesome-icon
                    icon="fa-solid fa-arrow-rotate-right"
                    class="mr-1"
                  />No, reload</b-button
                >
              </b-col>
            </b-row>
          </b-card-footer>
        </b-card>
      </b-col>
      <b-col cols="3"></b-col>
    </b-row>
    <b-row v-if="eventInfoLoaded() && isActiveMode()">
      <b-row
        class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
      >
        <b-col>
          <b-row>
            <b-col>
              {{ event.category }}:<br />
              <strong>
                <b-link
                  :href="getVocaDBEventUrl(event.id, event.urlSlug)"
                  target="_blank"
                  >{{ event.name }}
                </b-link>
              </strong>
            </b-col>
            <b-col>
              Held on:<br />
              <strong>
                <font-awesome-icon icon="fa-solid fa-calendar" class="mr-1" />
                {{ event.date.toLocaleString()
                }}<span v-if="event.endDate !== null">
                  - {{ event.endDate.toLocaleString() }}</span
                ></strong
              >
            </b-col>
            <b-col>
              Songs found:<br />
              <strong>{{ totalVideoCount }}</strong>
            </b-col>
            <b-col class="my-auto">
              <b-dropdown
                :disabled="defaultDisableCondition() || !isActiveMode()"
                block
                :text="getMaxResultsForDisplay()"
                class="my-auto"
                variant="primary"
              >
                <b-dropdown-item
                  :disabled="maxResults === 10"
                  @click="setMaxResults(10)"
                  >10
                </b-dropdown-item>
                <b-dropdown-item
                  :disabled="maxResults === 25"
                  @click="setMaxResults(25)"
                  >25
                </b-dropdown-item>
                <b-dropdown-item
                  :disabled="maxResults === 50"
                  @click="setMaxResults(50)"
                  >50
                </b-dropdown-item>
                <b-dropdown-item
                  :disabled="maxResults === 100"
                  @click="setMaxResults(100)"
                  >100
                </b-dropdown-item>
              </b-dropdown>
            </b-col>
            <b-col class="my-auto">
              <b-dropdown
                block
                :disabled="defaultDisableCondition() || !isActiveMode()"
                :text="getSortingConditionForDisplay()"
                variant="primary"
              >
                <b-dropdown-item
                  v-for="(key, value) in sortingOptions"
                  :key="key"
                  :disabled="sortingCondition === value"
                  @click="setSortingCondition(value)"
                >
                  {{ sortingOptions[value] }}
                </b-dropdown-item>
              </b-dropdown>
            </b-col>
            <b-col class="my-auto">
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="otherEvents"
                @click="filterVideos()"
                >Songs with other events
              </b-button>
            </b-col>
          </b-row>
        </b-col>
      </b-row>
      <b-row v-if="eventInfoLoaded" class="col-12">
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
          <b-th class="col-4 align-middle">Proposed actions</b-th>
        </b-thead>
        <b-tbody v-if="!allInvisible()">
          <tr
            v-for="item in entries.filter(item1 => item1.rowVisible)"
            :key="item.video.contentId"
          >
            <td>
              <b-form-checkbox
                v-if="!item.processed"
                v-model="item.toAssign"
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
                @click="item.embedVisible = !item.embedVisible"
              >
                <font-awesome-icon icon="fas fa-play" />
              </b-button>
              <b-link
                target="_blank"
                :href="getNicoVideoUrl(item.video.contentId)"
                v-html="item.video.title"
              ></b-link>
              <div>
                <b-badge
                  v-for="(item1, key1) in item.video.tags"
                  :key="key1"
                  v-clipboard:copy="item1.name"
                  class="m-sm-1"
                  :variant="item1.variant"
                  href="#"
                >
                  <font-awesome-icon icon="fas fa-tag" class="mr-1" />
                  {{ item1.name }}
                </b-badge>
              </div>
              <b-collapse
                :visible="item.embedVisible && !fetching"
                class="mt-2 collapsed"
              >
                <nico-embed
                  v-if="item.embedVisible && !fetching"
                  :content-id="item.video.contentId"
                />
              </b-collapse>
            </td>
            <td>
              <span v-if="hasReleaseEvent(item)" class="font-weight-bolder">
                <b-badge
                  v-if="
                    item.songEntry !== null &&
                    item.songEntry.releaseEvent !== null
                  "
                  class="badge text-center"
                  :variant="
                    item.songEntry.releaseEvent.id === event.id
                      ? 'success'
                      : 'warning'
                  "
                  :href="
                    getVocaDBEventUrl(
                      item.songEntry.releaseEvent.id,
                      item.songEntry.releaseEvent.urlSlug
                    )
                  "
                  target="_blank"
                >
                  {{ item.songEntry.releaseEvent.name }}
                </b-badge>
              </span>
              <span v-else class="text-muted">Unspecified</span>
            </td>
            <td v-if="item.songEntry != null">
                    <b-link
                      target="_blank"
                      :href="getVocaDBEntryUrl(item.songEntry.id)"
                      v-html="item.songEntry.name"
                    ></b-link>
                    <b-link
                      target="_blank"
                      :href="getVocaDBEntryUrl(item.songEntry.id)"
                    >
                      <b-badge
                        class="badge text-center ml-2"
                        :variant="
                      getSongTypeColorForDisplay(item.songEntry.songType)
                    "
                      >
                        {{ getShortenedSongType(item.songEntry.songType) }}
                      </b-badge>
                    </b-link>
                    <div class="text-muted mb-2">
                      {{ item.songEntry.artistString }}
                    </div>
              <span v-if="hasPublishDate(item)">
                    <font-awesome-icon
                      icon="fa-solid fa-calendar"
                      class="mr-1"
                    />{{ getReleaseDateFormatted(item.songEntry.publishDate) }}
                  </span>
              <span v-else class="text-muted">Unspecified</span>
                    <b-badge
                      :variant="
                      getDispositionBadgeColorVariant(
                        item.songEntry.eventDateComparison.disposition
                      )
                    "
                      class="mr-1 ml-3"
                    >
                      {{ item.songEntry.eventDateComparison.disposition }}
                    </b-badge>
                    <span
                      v-if="
                      item.songEntry.eventDateComparison.disposition !==
                        'unknown' &&
                      item.songEntry.eventDateComparison.disposition !==
                        'perfect'
                    "
                    >(by
                    {{ item.songEntry.eventDateComparison.dayDiff }}
                    day(s))
                  </span>
            </td>
            <td v-else>
              <b-button
                size="sm"
                :disabled="fetching"
                :href="getVocaDBAddSongUrl(item.video.contentId)"
                target="_blank"
              >Add to the database
              </b-button>
              <div
                v-if="item.publisher !== null"
                class="small text-secondary"
              >
                Published by
                <b-link
                  target="_blank"
                  :href="getVocaDBArtistUrl(item.publisher.id)"
                >{{ item.publisher.name.displayName }}</b-link
                >
              </div>
            </td>
            <td>
              <b-row>
                <b-col cols="10">
                  <ol class="ml-n4">
                    <li
                      v-if="
                        hasReleaseEvent(item) &&
                        item.songEntry.releaseEvent.id !== event.id &&
                        !isTaggedWithMultipleEvents(item)
                      "
                    >
                      Tag with "<b-link
                        target="_blank"
                        :href="getVocaDBTagUrl(8275, 'multiple-events')"
                        >multiple events</b-link
                      >" and update description
                    </li>
                    <li v-else-if="!hasReleaseEvent(item)">
                      Set "<b-link
                        :href="getVocaDBEventUrl(event.id, event.urlSlug)"
                        target="_blank"
                        >{{ event.name }}</b-link
                      >" as release event
                    </li>
                    <li
                      v-else-if="
                        hasReleaseEvent(item) &&
                        item.songEntry.releaseEvent.id !== event.id &&
                        isTaggedWithMultipleEvents(item)
                      "
                    >
                      <span class="text-danger text-monospace">Important:</span>
                      check that description mentions current event
                    </li>
                  </ol>
                </b-col>
                <b-col>
                  <b-button
                    v-if="item.processed"
                    disabled
                    class="btn"
                    variant="success"
                    @click="processSong(item)"
                  >
                    <font-awesome-icon icon="fas fa-check" />
                  </b-button>
                  <b-button
                    v-else
                    :disabled="defaultDisableCondition()"
                    class="btn"
                    variant="outline-success"
                    @click="processSong(item)"
                  >
                    <font-awesome-icon icon="fas fa-play" />
                  </b-button>
                </b-col>
              </b-row>
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
          <b-th class="col-4 align-middle">Proposed actions</b-th>
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

      <b-row v-if="eventInfoLoaded()" class="col-12">
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
import {
  DateComparisonResult,
  MinimalTag,
  ReleaseEventForDisplay
} from "@/backend/dto";
import { api } from "@/backend";
import { DateTime } from "luxon";
import Component from "vue-class-component";
import { Prop } from "vue-property-decorator";
import {
  getShortenedSongType,
  getVocaDBEventUrl,
  getVocaDBEntryUrl,
  getVocaDBTagUrl,
  getDateDisposition,
  getMaxResultsForDisplay,
  getSortingConditionForDisplay,
  getSongTypeColorForDisplay,
  getSongTypeStatsForDisplay,
  pageStateIsValid,
  infoLoaded,
  getDispositionBadgeColorVariant,
  EntryWithReleaseEventAndVisibility,
  SongType,
  getUniqueElementId,
  allVideosInvisible,
  fillReleaseEventForDisplay,
  getNicoTagUrl,
  defaultScopeTagString,
  getSortingConditionForDisplayNico,
  VideoWithEntryAndVisibility,
  getNicoVideoUrl,
  getVocaDBArtistUrl,
  getVocaDBAddSongUrl
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import NicoEmbed from "@/components/NicoEmbed.vue";

@Component({ components: { ErrorMessage, NicoEmbed } })
export default class extends Vue {
  @Prop()
  private readonly mode!: number;

  @Prop()
  private readonly thisMode!: number;

  // main variables
  private readonly event: ReleaseEventForDisplay = {
    id: -1,
    name: "",
    category: "",
    date: null,
    endDate: null,
    urlSlug: "",
    nndTags: []
  };
  private entries: VideoWithEntryAndVisibility[] = [];
  private tag: MinimalTag = { name: "", id: -1, urlSlug: "" };
  private eventName: string = "";
  private scopeTagString: string = "";
  private scopeTagStringFrozen: string = "";
  private eventTagsJoint: string = "";

  // api variables
  private pageToJump: number = 1;
  private startOffset: number = 0;
  private maxResults: number = 10;
  private sortingCondition = "startTime";
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;

  // interface variables
  private tagsLoaded: boolean = false;
  private tagsConfirmed: boolean = false;
  private allChecked: boolean = false;
  private showCollapse: boolean = false;
  private totalVideoCount: number = 0;
  private otherEvents: boolean = true;
  private page: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 0;
  private showVideosWithUploaderEntry: boolean = false;

  // error handling
  private alertCode: number = 0;
  private alertMessage: string = "";

  // interface dictionaries
  private songTypes: SongType[] = [
    { name: "Unspecified", show: true },
    { name: "Original", show: true },
    { name: "Remaster", show: true },
    { name: "Remix", show: true },
    { name: "Cover", show: true },
    { name: "Instrumental", show: true },
    { name: "Mashup", show: true },
    { name: "MusicPV", show: true },
    { name: "DramaPV", show: true },
    { name: "Other", show: true }
  ];

  private sortingOptions = {
    startTime: "upload time",
    viewCounter: "views",
    lengthSeconds: "length"
  };

  // proxy methods
  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(songType);
  }

  private getVocaDBEventUrl(id: number, urlSlug: string): string {
    return getVocaDBEventUrl(id, urlSlug);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBEntryUrl(id);
  }

  private getVocaDBTagUrl(id: number, urlSlug: string): string {
    return getVocaDBTagUrl(id, urlSlug);
  }

  private getNicoVideoUrl(contentId: string): string {
    return getNicoVideoUrl(contentId);
  }

  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getSortingConditionForDisplay(): string {
    return getSortingConditionForDisplayNico(this.sortingCondition);
  }

  private getVocaDBArtistUrl(artistId: number): string {
    return getVocaDBArtistUrl(artistId);
  }

  private getVocaDBAddSongUrl(contentId: string): string {
    return getVocaDBAddSongUrl(contentId);
  }

  private getSongTypeColorForDisplay(typeString: string): string {
    return getSongTypeColorForDisplay(typeString);
  }

  private getNicoTagUrl(tag: string): string {
    return getNicoTagUrl(tag, "");
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private eventInfoLoaded(): boolean {
    return (
      this.tagsConfirmed && infoLoaded(this.entries.length, this.event.name)
    );
  }

  private getDispositionBadgeColorVariant(disposition: string): string {
    return getDispositionBadgeColorVariant(disposition);
  }

  private allInvisible(): boolean {
    return allVideosInvisible(this.entries);
  }

  private getDateDisposition(
    date: DateTime | null,
    dateStart: DateTime,
    dateEnd: DateTime | null
  ): DateComparisonResult {
    return getDateDisposition(date, dateStart, dateEnd);
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

  private getHiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private toggleCheckAll(): void {
    for (const item of this.entries.filter(
      value => value.rowVisible && !value.processed
    )) {
      item.toAssign = this.allChecked;
    }
  }

  private countChecked(): number {
    return this.entries.filter(item => item.toAssign).length;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
  }

  private setSortingCondition(value: string): void {
    this.sortingCondition = value;
  }

  private filterVideos(): void {
    for (const item of this.entries) {
      item.rowVisible = true;
      item.toAssign = item.toAssign && item.rowVisible;
    }
  }

  private hasReleaseEvent(video: VideoWithEntryAndVisibility): boolean {
    return video.songEntry != null && video.songEntry.releaseEvent != null;
  }

  private hasPublishDate(video: VideoWithEntryAndVisibility): boolean {
    return video.songEntry != null && video.songEntry.publishDate != null;
  }

  private isTaggedWithMultipleEvents(
    video: VideoWithEntryAndVisibility
  ): boolean {
    return video.songEntry != null && video.songEntry.taggedWithMultipleEvents;
  }

  private getReleaseDateFormatted(releaseDate: string): string {

      return DateTime.fromISO(releaseDate).toLocaleString();

  }

  // error handling
  private processError(err: any): void {
    this.$bvToast.show(getUniqueElementId("error_", this.thisMode.toString()));
    if (err.response == undefined) {
      this.alertCode = 0;
      this.alertMessage = err.message;
    } else {
      this.alertCode = err.response.data.code;
      this.alertMessage = err.response.data.message;
    }
  }

  // api methods
  async fetchEvent(eventName: string): Promise<void> {
    if (eventName == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      let response = await api.fetchReleaseEventWithNndTags({
        eventName: eventName
      });
      fillReleaseEventForDisplay(response.event, this.event);
      this.event.nndTags = response.tags;
      this.eventTagsJoint = this.event.nndTags.join(" ");
    } catch (err) {
      this.processError(err);
    } finally {
      this.fetching = false;
      this.tagsLoaded = true;
    }
  }

  async fetch(
    targetTag: string,
    scopeString: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    if (targetTag == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      let response = await api.fetchVideosByEventNndTags({
        tags: targetTag,
        scopeTag: scopeString,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.sortingCondition,
        eventId: this.event.id
      });
      console.log(response.items);
      for (const item of response.items) {
        let temp: VideoWithEntryAndVisibility = {
          video: item.video,
          songEntry: item.songEntry,
          embedVisible: false,
          rowVisible: true,
          toAssign: false,
          publisher: item.publisher,
          processed: item.processed
        };
        if (
          temp.songEntry != null &&
          item.songEntry != null &&
          item.songEntry.publishDate != null
        ) {
          temp.songEntry.eventDateComparison = this.getDateDisposition(
            DateTime.fromISO(item.songEntry.publishDate),
            this.event.date!,
            this.event.endDate
          );
        }
      }
      this.entries = response.items.map(vid => {
        return {
          video: vid.video,
          songEntry: vid.songEntry,
          embedVisible: false,
          rowVisible: true,
          toAssign: false,
          publishDate: null,
          eventDateComparison: null,
          taggedWithMultipleEvents: false,
          publisher: vid.publisher,
          processed: vid.processed
        };
      });
      this.filterVideos();
      this.totalVideoCount = response.totalVideoCount;
      this.scopeTagString = response.safeScope;
      this.scopeTagStringFrozen = response.safeScope;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.allChecked = false;
    } catch (err) {
      this.processError(err);
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  private async processSong(song: VideoWithEntryAndVisibility): Promise<void> {
    if (song.songEntry == null) {
      return;
    }
    this.assigning = true;
    try {
      await api.assignEvent({
        songId: song.songEntry.id,
        event: {
          name: this.event.name,
          id: this.event.id,
          urlSlug: this.event.urlSlug
        }
      });
      song.processed = true;
      song.toAssign = false;
    } catch (err) {
      this.processError(err);
    } finally {
      this.assigning = false;
    }
  }

  private async processMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      for (const item1 of this.entries.filter(item => item.toAssign)) {
        await this.processSong(item1);
        item1.toAssign = false;
      }
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
    }
  }

  private confirmAndLoad(): void {
    this.tagsConfirmed = true;
    this.loadPage(1);
  }

  private clear(): void {
    this.tagsLoaded = false;
    this.tagsConfirmed = false;
    this.entries = [];
  }

  private loadPage(page: number): void {
    this.fetch(
      this.eventTagsJoint,
      this.scopeTagString,
      (page - 1) * this.maxResults,
      page
    );
  }

  // session
  created(): void {
    let max_results = localStorage.getItem("max_results");
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
  }
}
</script>
