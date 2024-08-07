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
              style="width: 80px"
              :disabled="eventName === '' || defaultDisableCondition()"
              @click="fetchEvent(eventName)"
              >Load
            </b-button>
            <b-button
              v-if="fetching"
              variant="primary"
              style="width: 80px"
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
                    >Clear
                  </b-button>
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
              <b-form-checkbox
                v-model="showPerfectDispositionOnly"
                @change="filterVideos()"
              >
                Show only perfect matches time-wise
              </b-form-checkbox>
              <b-form-checkbox
                v-model="showIneligibleVideos"
                @change="filterVideos()"
              >
                Show ineligible entries
              </b-form-checkbox>
              <b-form-checkbox
                v-if="showIneligibleVideos"
                v-model="allowIneligibleVideos"
                @change="toggleCheckAll()"
                >Allow processing ineligible entries
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
              <b-link
                :href="getVocaDBEventUrl(event.id, event.urlSlug)"
                target="_blank"
                >{{ event.name }}
              </b-link>
              :
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
              <div v-if="event.date != null" class="mb-3 text-secondary">
                <b-form-checkbox v-model="filterByEventDates"
                  >Search strictly within the event date boundaries
                </b-form-checkbox>
              </div>
              <div v-else class="mb-3 text-danger">
                <font-awesome-icon
                  icon="fa-solid fa-circle-exclamation"
                  class="mr-1"
                />
                Event date is not specified. Please fill in the event date and
                reload.
              </div>
              <span v-if="event.date != null">All good?</span>
              <b-row class="flex-fill mt-3">
                <b-col v-if="event.date != null" cols="6">
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
              Videos found (<span v-if="filterByEventDates">filtered</span
              ><span v-else>not filtered</span>):<br />
              <strong>{{ totalVideoCount }}</strong>
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
                :pressed.sync="showVideosWithoutEntries"
                @click="filterVideos()"
                >Videos without entries
              </b-button>
            </b-col>
          </b-row>
        </b-col>
      </b-row>
      <b-row v-if="eventInfoLoaded" class="col-12 m-auto">
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
          <b-th class="col-2 align-middle">Current release events</b-th>
          <b-th class="col-3 align-middle">Release date</b-th>
          <b-th class="col-3 align-middle">Proposed actions</b-th>
          <b-th class="col-1"></b-th>
        </b-thead>
        <b-tbody v-if="!allInvisible()">
          <tr
            v-for="item in entries.filter(item1 => item1.rowVisible)"
            :key="item.video.contentId"
          >
            <td>
              <b-form-checkbox
                v-if="isSelectable(item)"
                v-model="item.toAssign"
                :disabled="defaultDisableCondition() || !hasPublishDate(item)"
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
                >{{ item.video.title }}
              </b-link>
              <b-badge
                v-clipboard:copy="item.video.duration"
                variant="primary"
                class="m-sm-1"
                href="#"
                >{{ item.video.duration }}</b-badge
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
              <nico-description
                :content-id="item.video.contentId"
                :description="item.video.description"
                :publisher="item.video.publisher"
              />
            </td>
            <td>
              <span
                v-if="
                  item.songEntry != null &&
                  item.songEntry.releaseEvents.length > 0
                "
              >
                <div
                  v-for="(releaseEvent, key1) in item.songEntry.releaseEvents"
                  :key="key1"
                  class="font-weight-bolder"
                >
                  <b-badge
                    class="badge text-center"
                    :variant="getEventColorVariant(releaseEvent)"
                    :href="
                      getVocaDBEventUrl(releaseEvent.id, releaseEvent.urlSlug)
                    "
                    target="_blank"
                  >
                    {{ releaseEvent.name }}
                  </b-badge>
                </div>
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
                >{{ item.songEntry.name }}
                <b-badge
                  class="badge text-center ml-2"
                  :variant="getSongTypeColorForDisplay(item.songEntry.songType)"
                >
                  {{ getShortenedSongType(item.songEntry.songType) }}
                </b-badge>
              </b-link>
              <div class="text-muted mb-2">
                {{ item.songEntry.artistString }}
              </div>
              <date-disposition
                v-if="hasPublishDate(item)"
                :release-date="item.songEntry.publishDate"
                :event-date-comparison="item.songEntry.eventDateComparison"
                :event-id-in-description="item.songEntry.eventIdInDescription"
                :delta="timeDelta"
              />
              <b-badge v-else variant="danger"
                >publish date not specified
              </b-badge>
            </td>
            <td v-else>
              <date-disposition
                :release-date="item.video.startTime"
                :event-date-comparison="item.video.eventDateComparison"
                :published-in-time="item.video.eventDateComparison.eligible"
                :event-id-in-description="false"
                :delta="timeDelta"
              />
            </td>
            <td>
              <div v-if="item.songEntry !== null" class="mb-2">
                <action
                  v-if="!item.processed"
                  :process-item="
                    !item.processed &&
                    (isEligible(item) ||
                      (isEarly(item) && allowIneligibleVideos))
                  "
                  :name="event.name"
                  :link="getVocaDBEventUrl(event.id, event.urlSlug)"
                  :eligible="isEligible(item)"
                />
              </div>
              <div v-else class="mb-2">
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
                    >{{ item.publisher.name }}
                  </b-link>
                </div>
              </div>
            </td>
            <td class="text-center">
              <span v-if="item.songEntry !== null">
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
                  :disabled="
                    defaultDisableCondition() ||
                    !hasPublishDate(item) ||
                    !isSelectable(item)
                  "
                  class="btn"
                  variant="outline-success"
                  @click="processSong(item)"
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
  ReleaseEventForApiContractSimplified,
  ReleaseEventForDisplay
} from "@/backend/dto";
import { api } from "@/backend";
import { DateTime } from "luxon";
import Component from "vue-class-component";
import { Prop } from "vue-property-decorator";
import {
  allVideosInvisible,
  DateComparisonResult,
  defaultScopeTagString,
  fillReleaseEventForDisplay,
  getDateDisposition,
  getEventColorVariant,
  getMaxResultsForDisplay,
  getNicoTagUrl,
  getNicoVideoUrl,
  getShortenedSongType,
  getSongTypeColorForDisplay,
  getSortingConditionForDisplayNico,
  getUniqueElementId,
  getVocaDBAddSongUrl,
  getVocaDBArtistUrl,
  getVocaDBEntryUrl,
  getVocaDBEventUrl,
  getVocaDBTagUrl,
  isEarly,
  isEligible,
  pageStateIsValid,
  SongType,
  VideoWithEntryAndVisibility
} from "@/utils";
import ErrorMessage from "@/components/ErrorMessage.vue";
import NicoEmbed from "@/components/NicoEmbed.vue";
import DateDisposition from "@/components/DateDisposition.vue";
import { AxiosResponse } from "axios";
import Action from "@/components/Action.vue";
import NicoDescription from "@/components/NicoDescription.vue";

@Component({
  methods: {
    getShortenedSongType,
    getNicoVideoUrl,
    getSongTypeColorForDisplay
  },
  components: {
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
  private dbAddress: string = "";

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
  private showIneligibleVideos: boolean = false;
  private allowIneligibleVideos: boolean = false;
  private showPerfectDispositionOnly: boolean = false;
  private timeDelta: number = 0;
  private filterByEventDates: boolean = true;
  private participantLink = this.getVocaDBTagUrl(9141, "event-participant");
  private multipleEventsLink = this.getVocaDBTagUrl(8275, "multiple-events");

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

  private getVocaDBEventUrl(id: number, urlSlug: string): string {
    return getVocaDBEventUrl(this.dbAddress, id, urlSlug);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBEntryUrl(this.dbAddress, id);
  }

  private getVocaDBTagUrl(id: number, urlSlug: string): string {
    return getVocaDBTagUrl(this.dbAddress, id, urlSlug);
  }

  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getSortingConditionForDisplay(): string {
    return getSortingConditionForDisplayNico(this.sortingCondition);
  }

  private getVocaDBArtistUrl(artistId: number): string {
    return getVocaDBArtistUrl(this.dbAddress, artistId);
  }

  private getVocaDBAddSongUrl(contentId: string): string {
    return getVocaDBAddSongUrl(this.dbAddress, contentId);
  }

  private getNicoTagUrl(tag: string): string {
    return getNicoTagUrl(tag, "");
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private eventInfoLoaded(): boolean {
    return this.tagsConfirmed && this.numOfPages > 0;
  }

  private allInvisible(): boolean {
    return allVideosInvisible(this.entries);
  }

  private getDateDisposition(
    date: DateTime | null,
    dateStart: DateTime,
    dateEnd: DateTime | null
  ): DateComparisonResult {
    return getDateDisposition(date, dateStart, dateEnd, this.timeDelta);
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
      item.toAssign =
        item.rowVisible &&
        this.isSelectable(item) &&
        this.hasPublishDate(item) &&
        this.allChecked;
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
    return video.songEntry != null && video.songEntry.releaseEvents != null;
  }

  private hasPublishDate(video: VideoWithEntryAndVisibility): boolean {
    return video.songEntry != null && video.songEntry.publishDate != null;
  }

  private isEligible(video: VideoWithEntryAndVisibility): boolean {
    return isEligible(video.songEntry, video.video.eventDateComparison);
  }

  private isEarly(video: VideoWithEntryAndVisibility): boolean {
    return isEarly(video.songEntry, video.video.eventDateComparison);
  }

  private getEventColorVariant(
    event: ReleaseEventForApiContractSimplified
  ): string {
    return getEventColorVariant(event, this.event.id);
  }

  // row filtering
  private currentEventFilledFlag(item: VideoWithEntryAndVisibility): boolean {
    return (
      (this.currentEventFilled && this.showIneligibleVideosFlag(item)) ||
      item.songEntry == null ||
      !item.processed
    );
  }

  private showVideosWithoutEntriesFlag(
    item: VideoWithEntryAndVisibility
  ): boolean {
    return (
      (this.showVideosWithoutEntries && this.showIneligibleVideosFlag(item)) ||
      item.songEntry != null ||
      (this.showVideosWithUploaderEntry && item.publisher != null)
    );
  }

  private showIneligibleVideosFlag(item: VideoWithEntryAndVisibility): boolean {
    return this.showIneligibleVideos || this.isEligible(item);
  }

  private showPerfectDispositionOnlyFlag(
    item: VideoWithEntryAndVisibility
  ): boolean {
    return (
      (!this.showPerfectDispositionOnly &&
        this.showIneligibleVideosFlag(item)) ||
      (item.songEntry == null &&
        item.video.eventDateComparison?.disposition == "perfect") ||
      item.songEntry?.eventDateComparison.disposition == "perfect"
    );
  }

  private filterVideos(): void {
    for (const item of this.entries) {
      item.rowVisible =
        this.currentEventFilledFlag(item) &&
        this.showVideosWithoutEntriesFlag(item) &&
        this.showPerfectDispositionOnlyFlag(item);
      item.toAssign = item.toAssign && item.rowVisible;
    }
  }

  private isSelectable(item: VideoWithEntryAndVisibility): boolean {
    return (
      item.songEntry != null &&
      (this.allowIneligibleVideos || this.isEligible(item)) &&
      !item.processed
    );
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
    this.updateUrl(eventName);
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
      this.timeDelta = this.event.endDate == null ? 7 : 1;
      localStorage.setItem("vocadb_event_name", eventName);
      this.filterByEventDates = true;
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
      if (this.event.date == null) {
        return;
      }
      let startTime = this.event.date?.toISO();
      let endTime =
        this.event.endDate == null ? null : this.event.endDate.toISO();
      let response = await api.fetchVideosByEventNndTags({
        tags: targetTag,
        scopeTag: scopeString,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.sortingCondition,
        eventId: this.event.id,
        startTime: this.filterByEventDates ? startTime : null,
        endTime: endTime
      });
      for (const item of response.items) {
        if (item.songEntry != null && item.songEntry.publishDate != null) {
          const minDate = DateTime.min(
            DateTime.fromISO(item.songEntry.publishDate, { zone: "utc" }),
            DateTime.fromISO(item.video.startTime, { zone: "utc" })
          );
          item.songEntry.publishDate = minDate.toISO();
          item.songEntry.eventDateComparison = this.getDateDisposition(
            minDate,
            this.event.date!,
            this.event.endDate
          );
        }
        item.video.eventDateComparison = this.getDateDisposition(
          DateTime.fromISO(item.video.startTime, { zone: "utc" }),
          this.event.date!,
          this.event.endDate
        );
        if (item.songEntry != null && item.songEntry.publishDate != null) {
          item.songEntry.eventDateComparison = this.getDateDisposition(
            DateTime.fromISO(item.songEntry.publishDate, { zone: "utc" }),
            this.event.date!,
            this.event.endDate
          );
          if (
            !item.songEntry.eventDateComparison.eligible &&
            item.video.eventDateComparison.eligible
          ) {
            item.songEntry.eventDateComparison.eligible = true;
          }
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
          publisher: vid.publisher,
          processed: vid.processed
        };
      });
      this.filterVideos();
      this.totalVideoCount = response.totalVideoCount;
      this.scopeTagString = response.safeScope;
      this.scopeTagStringFrozen = response.safeScope;
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
      song.songEntry.releaseEvents.push({
        id: this.event.id,
        date: null,
        nndTags: this.event.nndTags,
        name: this.event.name,
        urlSlug: this.event.urlSlug,
        category: this.event.category,
        endDate: null
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
    this.numOfPages = 0;
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
    let event_name = localStorage.getItem("vocadb_event_name");
    if (event_name != null) {
      this.eventName = event_name;
    }
    let sort_by = localStorage.getItem("sort_by");
    if (sort_by != null) {
      this.sortingCondition = sort_by;
    }
    let dbAddress = localStorage.getItem("dbAddress");
    if (this.dbAddress == "" && dbAddress != null) {
      this.dbAddress = dbAddress;
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
