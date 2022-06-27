<template>
  <div>
    <error-message
      :alert-code="alertCode"
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
            :disabled="defaultDisableCondition()"
            placeholder="Event name (VocaDB)"
            @keydown.enter.native="fetchEvent(eventName)"
          >
          </b-form-input>
          <template #append>
            <b-button
              v-if="!fetching"
              variant="primary"
              style="width: 80px"
              :disabled="eventName === '' || defaultDisableCondition()"
              @click="fetchEvent(eventName)"
              >Load</b-button
            >
            <b-button
              v-if="fetching"
              variant="primary"
              style="width: 80px"
              disabled
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
          <b-row
            v-if="event.name !== '' && isActiveMode() && tagsConfirmed"
            class="mt-2"
          >
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
          <b-row
            v-if="event.name !== '' && isActiveMode() && tagsConfirmed"
            class="mt-2"
          >
            <b-col>
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="showVideosWithoutEntries"
                @click="filterVideos()"
                >Videos without entries
              </b-button>
            </b-col>
            <b-col>
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="showVideosWithNoEvents"
                @click="filterVideos()"
                >Songs with no events
              </b-button>
            </b-col>
          </b-row>
          <b-row
            v-if="event.name !== '' && isActiveMode() && tagsConfirmed"
            class="mt-2"
          >
            <b-col>
              <b-form-checkbox
                v-model="showVideosWithUploaderEntry"
                @change="filterVideos()"
              >
                Force show videos whose uploaders have entries at VocaDB
              </b-form-checkbox>
            </b-col>
          </b-row>
          <b-row class="mt-2">
            <b-col>
              <template>
                <b-input-group inline>
                  <b-form-input
                    v-model="timeDelta"
                    :disabled="defaultDisableCondition() || !isActiveMode()"
                    number
                    type="range"
                    min="0"
                    :max="event.endDate == null ? 7 : 1"
                    @change="filterEntriesIfValidState()"
                  />
                  <template #prepend>
                    <b-input-group-text class="justify-content-center">
                      <b-form-checkbox
                        v-model="timeDeltaEnabled"
                        :disabled="fetching || !isActiveMode()"
                        @change="filterEntriesIfValidState()"
                      />
                      Time delta
                    </b-input-group-text>
                  </template>
                  <template #append>
                    <b-input-group-text class="justify-content-center"
                      >{{ timeDelta }}
                      day(s)
                    </b-input-group-text>
                  </template>
                </b-input-group>
                <b-input-group inline class="mt-2 text-center">
                  <b-checkbox
                    v-model="timeDeltaBefore"
                    :state="getTimeDeltaState()"
                    :disabled="!timeDeltaEnabled"
                    class="col-6"
                    @change="filterEntriesIfValidState()"
                    >before</b-checkbox
                  >
                  <b-checkbox
                    v-model="timeDeltaAfter"
                    :state="getTimeDeltaState()"
                    :disabled="!timeDeltaEnabled"
                    class="col-6"
                    @change="filterEntriesIfValidState()"
                    >after</b-checkbox
                  >
                </b-input-group>
              </template>
            </b-col>
          </b-row>
        </b-collapse>
      </b-col>
      <b-col class="m-auto text-left mt-lg-3 ml-n1"><b-button
        v-if="eventInfoLoaded()"
        variant="link"
        :disabled="defaultDisableCondition()"
        @click="loadPage(page)"
      ><font-awesome-icon
        icon="fa-solid fa-arrow-rotate-right"
      /></b-button></b-col>
    </b-row>
    <Transition appear>
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
              <div v-for="(tag, key) in event.nndTags" :key="key">
                <b-link :href="getNicoTagUrl(tag)" target="_blank"
                  ><font-awesome-icon icon="fas fa-tag" class="mr-1" />{{
                    tag
                  }}</b-link
                >
              </div>
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
              Videos found:<br />
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
                :pressed.sync="showVideosWithOtherEvents"
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
          <b-th class="col-3 align-middle">Video</b-th>
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
                v-if="!item.processed && item.songEntry !== null"
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
                >{{ item.video.title }}</b-link
              >
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
              <span v-else-if="item.songEntry != null" class="text-muted"
                >Unspecified</span
              >
              <span v-else class="text-muted">None</span>
            </td>
            <td v-if="item.songEntry != null">
              <b-link
                target="_blank"
                :href="getVocaDBEntryUrl(item.songEntry.id)"
                >{{ item.songEntry.name
                }}<b-badge
                  class="badge text-center ml-2"
                  :variant="getSongTypeColorForDisplay(item.songEntry.songType)"
                >
                  {{ getShortenedSongType(item.songEntry.songType) }}
                </b-badge></b-link
              >
              <div class="text-muted mb-2">
                {{ item.songEntry.artistString }}
              </div>
              <date-disposition
                v-if="hasPublishDate(item)"
                :release-date="item.songEntry.publishDate"
                :event-date-comparison="item.songEntry.eventDateComparison"
              />
            </td>
            <td v-else>
              <date-disposition
                :release-date="item.video.startTime"
                :event-date-comparison="item.video.eventDateComparison"
              />
            </td>
            <td v-if="item.songEntry !== null">
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
                    :disabled="
                      defaultDisableCondition() ||
                      (hasReleaseEvent(item) &&
                        item.songEntry.releaseEvent.id !== event.id &&
                        isTaggedWithMultipleEvents(item))
                    "
                    class="btn"
                    variant="outline-success"
                    @click="processSong(item)"
                  >
                    <font-awesome-icon icon="fas fa-play" />
                  </b-button>
                </b-col>
              </b-row>
            </td>
            <td v-else>
              <b-button
                size="sm"
                :disabled="fetching"
                :href="getVocaDBAddSongUrl(item.video.contentId)"
                target="_blank"
                >Add to the database
              </b-button>
              <div v-if="item.publisher !== null" class="small text-secondary">
                Published by
                <b-link
                  target="_blank"
                  :href="getVocaDBArtistUrl(item.publisher.id)"
                  >{{ item.publisher.name.displayName }}</b-link
                >
              </div>
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
          <b-th class="col-3 align-middle">Video</b-th>
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
import { ReleaseEventForDisplay } from "@/backend/dto";
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
  getSongTypeColorForDisplay,
  pageStateIsValid,
  infoLoaded,
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
  getVocaDBAddSongUrl,
  getTimeDeltaState,
  DateComparisonResult,
  dateIsWithinTimeDelta
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import NicoEmbed from "@/components/NicoEmbed.vue";
import DateDisposition from "@/components/DateDisposition.vue";
import { AxiosResponse } from "axios";

@Component({ components: { ErrorMessage, NicoEmbed, DateDisposition } })
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
  private currentEventFilled: boolean = true;
  private page: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 0;
  private showVideosWithoutEntries: boolean = true;
  private showVideosWithUploaderEntry: boolean = false;
  private showVideosWithOtherEvents: boolean = true;
  private showVideosWithNoEvents: boolean = true;
  private timeDeltaEnabled: boolean = false;
  private timeDeltaBefore: boolean = false;
  private timeDeltaAfter: boolean = false;
  private timeDelta: number = 0;

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

  private getTimeDeltaState(): boolean {
    return getTimeDeltaState(
      this.timeDeltaEnabled,
      this.timeDeltaBefore,
      this.timeDeltaAfter,
      this.timeDelta
    );
  }

  private filterEntriesIfValidState(): void {
    if (this.getTimeDeltaState()) {
      if (!this.timeDeltaEnabled) {
        this.timeDeltaBefore = false;
        this.timeDeltaAfter = false;
      }
      this.filterVideos();
    }
  }

  // row filtering
  private currentEventFilledFlag(item: VideoWithEntryAndVisibility): boolean {
    return (
      this.currentEventFilled ||
      item.songEntry == null ||
      item.songEntry.releaseEvent == null ||
      item.songEntry.releaseEvent.id != this.event.id
    );
  }

  private showVideosWithoutEntriesFlag(
    item: VideoWithEntryAndVisibility
  ): boolean {
    return (
      this.showVideosWithoutEntries ||
      item.songEntry != null ||
      (this.showVideosWithUploaderEntry && item.publisher != null)
    );
  }

  private showVideosWithOtherEventsFlag(
    item: VideoWithEntryAndVisibility
  ): boolean {
    return (
      this.showVideosWithOtherEvents ||
      item.songEntry == null ||
      item.songEntry.releaseEvent == null ||
      item.songEntry.releaseEvent.id == this.event.id
    );
  }

  private showVideosWithNoEventsFlag(
    item: VideoWithEntryAndVisibility
  ): boolean {
    return (
      this.showVideosWithNoEvents ||
      item.songEntry == null ||
      item.songEntry.releaseEvent != null
    );
  }

  private timeDeltaFlag(eventDateComparison: DateComparisonResult): boolean {
    return (
      !this.timeDeltaEnabled ||
      dateIsWithinTimeDelta(
        this.timeDelta,
        this.timeDeltaBefore,
        this.timeDeltaAfter,
        eventDateComparison
      )
    );
  }

  private filterVideos(): void {
    for (const item of this.entries) {
      item.rowVisible =
        this.currentEventFilledFlag(item) &&
        this.showVideosWithoutEntriesFlag(item) &&
        this.showVideosWithOtherEventsFlag(item) &&
        this.showVideosWithNoEventsFlag(item) &&
        this.timeDeltaFlag(
          item.songEntry != null
            ? item.songEntry!.eventDateComparison
            : item.video.eventDateComparison!
        );
      item.toAssign = item.toAssign && item.rowVisible;
    }
  }

  // error handling
  private processError(
    err: { response: AxiosResponse } | { response: undefined; message: string }
  ): void {
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
    this.tagsLoaded = false;
    this.tagsConfirmed = false;
    this.entries = [];
    this.fetching = true;
    try {
      let response = await api.fetchReleaseEventWithNndTags({
        eventName: eventName
      });
      fillReleaseEventForDisplay(response.event, this.event);
      this.event.nndTags = response.tags;
      this.eventTagsJoint = this.event.nndTags.join(" OR ");
      this.tagsLoaded = true;
    } catch (err) {
      this.processError(err);
    } finally {
      this.fetching = false;
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
          item.songEntry.eventDateComparison = this.getDateDisposition(
            DateTime.fromISO(item.songEntry.publishDate),
            this.event.date!,
            this.event.endDate
          );
        } else if (temp.songEntry == null && item.songEntry == null) {
          item.video.eventDateComparison = this.getDateDisposition(
            DateTime.fromISO(item.video.startTime),
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
      if (song.songEntry.releaseEvent == null) {
        song.songEntry.releaseEvent = {
          id: this.event.id,
          date: null,
          nndTags: this.event.nndTags,
          name: this.event.name,
          urlSlug: this.event.urlSlug,
          category: this.event.category,
          endDate: null
        };
      }
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

<style lang="scss">
@import "../style.scss";
</style>
