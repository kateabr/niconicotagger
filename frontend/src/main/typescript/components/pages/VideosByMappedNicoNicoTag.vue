<template>
  <span>
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
              v-b-toggle="'scope-collapse-' + thisMode"
              variant="primary"
              style="width: 80px"
              :disabled="defaultDisableCondition()"
              ><font-awesome-icon
                class="mr-sm-1"
                icon="fas fa-angle-down"
              />More</b-button
            >
          </template>
          <b-form-input
            id="tag-form"
            v-model="tagName"
            :disabled="defaultDisableCondition()"
            placeholder="NicoNicoDouga tag"
            @keydown.enter.native="loadInitialPage"
          >
          </b-form-input>
          <template #append>
            <b-button
              v-if="!fetching"
              variant="primary"
              style="width: 80px"
              :disabled="tagName === '' || defaultDisableCondition()"
              @click="loadInitialPage()"
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
              <b-input-group inline>
                <b-form-input
                  id="scope-tag-form"
                  v-model="scopeTagString"
                  placeholder="Specify tag scope (NND)"
                  :disabled="defaultDisableCondition()"
                  @keydown.enter.native="loadInitialPage"
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
          <b-row v-if="tagNameFrozen !== '' && isActiveMode()" class="mt-2">
            <b-col>
              <b-dropdown
                block
                :disabled="defaultDisableCondition()"
                :text="getsortingCondition()"
                variant="primary"
                menu-class="w-100"
              >
                <b-dropdown-item
                  v-for="(key, item) in orderOptionsNico"
                  :key="key"
                  :disabled="sortingCondition === item"
                  @click="setSortingCondition(item)"
                >
                  {{ orderOptionsNico[item] }}
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
          <b-row v-if="tagNameFrozen !== '' && isActiveMode()" class="mt-2">
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
    <b-row
      v-if="tagInfoLoaded() && isActiveMode()"
      class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="my-auto"
        >Tag:<br /><strong>
          <b-link
            v-if="tagInfoLoaded()"
            target="_blank"
            :href="getNicoTagUrl(tagNameFrozen, scopeTagStringFrozen)"
            >{{ tagNameFrozen }}
            <font-awesome-icon class="ml-1" icon="fas fa-external-link" />
          </b-link> </strong
      ></b-col>
      <b-col class="my-auto"
        >Mapped to:<br />
        <strong v-for="(tag, key) in tagMappings" :key="key">
          <b-link
            target="_blank"
            :href="getVocaDBTagUrl(tagInfo[key].id, tagInfo[key].urlSlug)"
            >{{ tag }}</b-link
          ><span v-if="tagMappings.length - key > 1">, </span>
        </strong>
      </b-col>
      <b-col class="my-auto"
        >Videos found:<br /><strong>{{ totalVideoCount }}</strong></b-col
      >
      <b-col class="my-auto">
        <b-dropdown
          block
          :disabled="defaultDisableCondition()"
          :text="getResultNumberStr()"
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
          :pressed.sync="noEntry"
          @click="filterVideos()"
          >Videos without entries
        </b-button>
      </b-col>
      <b-col class="my-auto">
        <b-button
          :disabled="defaultDisableCondition()"
          variant="primary"
          block
          :pressed.sync="tagged"
          @click="filterVideos()"
          >Tagged songs
        </b-button>
      </b-col>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
      <b-col class="col-12">
        <div class="text-center pt-sm-3">
          <b-button-group>
            <b-button
              v-for="(type, key) in songTypes"
              :key="key"
              class="pl-4 pr-4"
              :disabled="defaultDisableCondition()"
              :variant="
                (type.show ? '' : 'outline-') +
                getSongTypeColorForDisplay(type.name)
              "
              @click="
                type.show = !type.show;
                filterVideos();
              "
              >{{ getSongTypeStatsForDisplay(type.name) }}
            </b-button>
          </b-button-group>
        </div>
      </b-col>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
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
          ></b-pagination>
        </div>
      </template>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
      <b-table-simple hover class="mt-1 col-lg-12">
        <b-thead>
          <b-th>
            <b-form-checkbox
              v-model="allChecked"
              size="lg"
              :disabled="defaultDisableCondition() || noVideosWithEntries()"
              @change="toggleCheckAll"
            ></b-form-checkbox>
          </b-th>
          <b-th class="col-8 align-middle">Video</b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-1 align-middle">Tag</b-th>
        </b-thead>
        <b-tbody v-if="!allInvisible()">
          <b-tr
            v-for="(item, key) in videos"
            :id="item.video.contentId"
            :key="key"
            :style="item.rowVisible ? '' : 'display: none'"
          >
            <b-td>
              <div v-if="item.songEntry != null && !item.processed">
                <b-form-checkbox
                  v-model="item.toAssign"
                  size="lg"
                  :disabled="defaultDisableCondition()"
                ></b-form-checkbox>
              </div>
            </b-td>
            <b-td>
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
              <nico-description
                :content-id="item.video.contentId"
                :description="item.video.description"
              />
            </b-td>
            <b-td>
              <div v-if="item.songEntry != null">
                <b-link
                  target="_blank"
                  :href="getVocaDBEntryUrl(item.songEntry.id)"
                  >{{ item.songEntry.name
                  }}<b-badge
                    class="badge text-center ml-2"
                    :variant="
                      getSongTypeColorForDisplay(item.songEntry.songType)
                    "
                  >
                    {{ getShortenedSongType(item.songEntry.songType) }}
                  </b-badge></b-link
                >
                <div class="text-muted">
                  {{ item.songEntry.artistString }}
                </div>
              </div>
              <div v-else>
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
              </div>
            </b-td>
            <b-td>
              <div v-if="item.songEntry != null">
                <b-button-toolbar key-nav>
                  <b-button
                    v-if="item.processed"
                    style="pointer-events: none"
                    class="btn disabled"
                    variant="success"
                  >
                    <font-awesome-icon icon="fas fa-check" />
                  </b-button>
                  <b-button
                    v-else
                    :disabled="defaultDisableCondition()"
                    class="btn"
                    variant="outline-success"
                    @click="assign(item)"
                  >
                    <font-awesome-icon icon="fas fa-plus" />
                  </b-button>
                </b-button-toolbar>
              </div>
            </b-td>
          </b-tr>
        </b-tbody>
        <b-tbody v-else>
          <b-tr>
            <b-td colspan="4" class="text-center text-muted">
              <small>No items to display</small>
            </b-td>
          </b-tr>
        </b-tbody>
        <b-tfoot>
          <b-th>
            <b-form-checkbox
              v-model="allChecked"
              size="lg"
              :disabled="defaultDisableCondition() || noVideosWithEntries()"
              @change="toggleCheckAll"
            ></b-form-checkbox>
          </b-th>
          <b-th class="col-8 align-middle">Video</b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-1 align-middle">Tag</b-th>
        </b-tfoot>
      </b-table-simple>
    </b-row>
    <b-row
      v-if="tagInfoLoaded() && isActiveMode()"
      class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="col-lg-3 m-auto">
        <b-button
          block
          variant="primary"
          :disabled="countChecked() === 0 || massAssigning || fetching"
          @click="assignMultiple"
        >
          <div v-if="massAssigning">
            <b-spinner small class="mr-1"></b-spinner>
            Assigning...
          </div>
          <div v-else>Batch assign ({{ countChecked() }} selected)</div>
        </b-button>
      </b-col>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
      <div class="overflow-auto m-auto mt-lg-4">
        <b-pagination
          v-model="page"
          class="mb-5"
          align="center"
          :total-rows="totalVideoCount"
          :per-page="maxResults"
          use-router
          first-number
          last-number
          limit="10"
          :disabled="defaultDisableCondition()"
          @change="loadPage"
        ></b-pagination>
      </div>
    </b-row>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { api } from "@/backend";
import { AssignableTag } from "@/backend/dto";
import {
  getShortenedSongType,
  getVocaDBEntryUrl,
  getVocaDBTagUrl,
  getMaxResultsForDisplay,
  getSortingConditionForDisplayNico,
  getSongTypeColorForDisplay,
  getSongTypeStatsForDisplay,
  pageStateIsValid,
  infoLoaded,
  defaultScopeTagString,
  SongType,
  getNicoTagUrl,
  allVideosInvisible,
  getNicoVideoUrl,
  getUniqueElementId,
  getVocaDBAddSongUrl,
  getVocaDBArtistUrl,
  VideoWithEntryAndVisibility
} from "@/utils";
import NicoEmbed from "@/components/NicoEmbed.vue";
import ErrorMessage from "@/components/ErrorMessage.vue";
import { AxiosResponse } from "axios";
import NicoDescription from "@/components/NicoDescription.vue";

@Component({ components: { NicoDescription, NicoEmbed, ErrorMessage } })
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly targName: string | undefined;

  // main variables
  private tagName: string = "";
  private tagNameFrozen: string = "";
  private scopeTagString: string = "";
  private scopeTagStringFrozen: string = "";
  private videos: VideoWithEntryAndVisibility[] = [];
  private tagInfo: AssignableTag[] = [];

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
  private allChecked: boolean = false;
  private showCollapse: boolean = false;
  private totalVideoCount: number = 0;
  private page: number = 0;
  private maxPage: number = 0;
  private numOfPages: number = 0;
  private noEntry: boolean = false;
  private showVideosWithUploaderEntry: boolean = true;
  private tagged: boolean = false;
  private tagMappings: string[] = [];

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

  private orderOptionsNico = {
    startTime: "upload time",
    viewCounter: "views",
    lengthSeconds: "length"
  };

  // error handling
  private alertCode: number = 0;
  private alertMessage: string = "";

  // proxy methods
  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private tagInfoLoaded(): boolean {
    return infoLoaded(this.videos.length, this.tagNameFrozen);
  }

  private getsortingCondition(): string {
    return getSortingConditionForDisplayNico(this.sortingCondition);
  }

  private setDefaultScopeTagString(): void {
    this.scopeTagString = defaultScopeTagString;
  }

  private getNicoTagUrl(tagName: string, scopeTag: string): string {
    return getNicoTagUrl(tagName, scopeTag);
  }

  private getVocaDBTagUrl(id: number, urlSlug: string): string {
    return getVocaDBTagUrl(this.dbAddress, id, urlSlug);
  }

  private getResultNumberStr(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getSongTypeColorForDisplay(songType: string): string {
    return getSongTypeColorForDisplay(songType);
  }

  private getSongTypeStatsForDisplay(songType: string): string {
    return getSongTypeStatsForDisplay(
      songType,
      this.videos.filter(
        video =>
          video.songEntry != null && video.songEntry?.songType == songType
      ).length
    );
  }

  private allInvisible(): boolean {
    return allVideosInvisible(this.videos);
  }

  private getNicoVideoUrl(contentId: string): string {
    return getNicoVideoUrl(contentId);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBEntryUrl(this.dbAddress, id);
  }

  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(songType);
  }

  private getVocaDBAddSongUrl(contentId: string): string {
    return getVocaDBAddSongUrl(this.dbAddress, contentId);
  }

  private getVocaDBArtistUrl(artistId: number): string {
    return getVocaDBArtistUrl(this.dbAddress, artistId);
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

  private setSortingCondition(value: string): void {
    this.sortingCondition = value;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
  }

  private noVideosWithEntries(): boolean {
    return this.videos.every(video => video.songEntry == null);
  }

  private toggleCheckAll(): void {
    for (const item of this.videos.filter(
      video => video.rowVisible && video.songEntry != null && !video.processed
    )) {
      item.toAssign = this.allChecked;
    }
  }

  private countChecked(): number {
    return this.videos.filter(video => video.toAssign).length;
  }

  // row filtering
  private hiddenTypeFlag(video: VideoWithEntryAndVisibility): boolean {
    return (
      this.getHiddenTypes() == 0 ||
      (video.songEntry != null &&
        !this.songTypes
          .filter(t => !t.show)
          .map(t => t.name)
          .includes(video.songEntry.songType))
    );
  }

  private noEntryFlag(video: VideoWithEntryAndVisibility): boolean {
    return (
      video.songEntry != null ||
      this.noEntry ||
      (video.publisher != null && this.showVideosWithUploaderEntry)
    );
  }

  private taggedFlag(video: VideoWithEntryAndVisibility): boolean {
    return video.songEntry == null || this.tagged || !video.processed;
  }

  private filterVideos(): void {
    for (const item of this.videos) {
      item.rowVisible =
        this.hiddenTypeFlag(item) &&
        this.noEntryFlag(item) &&
        this.taggedFlag(item);
      item.toAssign = item.toAssign && item.rowVisible;
    }
  }

  private updateUrl(): void {
    this.$router.push({
      name: "tags-full",
      params: { browseMode: this.thisMode, targName: this.tagName }
    });
  }

  // api methods
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
      let response = await api.fetchVideos({
        tag: targetTag,
        scopeTag: scopeString,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.sortingCondition
      });
      this.videos = response.items.map(vid => {
        return {
          video: vid.video,
          songEntry: vid.songEntry,
          embedVisible: false,
          rowVisible: true,
          toAssign: false,
          publisher: vid.publisher,
          processed: vid.processed
        };
      });
      this.filterVideos();
      this.tagMappings = response.tagMappings;
      this.totalVideoCount = response.totalVideoCount;
      this.scopeTagString = response.safeScope;
      this.scopeTagStringFrozen = response.safeScope;
      this.tagInfo = response.tags;
      this.tagName = targetTag;
      this.tagNameFrozen = targetTag;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.allChecked = false;
      localStorage.setItem("nicovideo_mapped_tag", targetTag);
      localStorage.setItem("nicovideo_mapped_tag_scope", scopeString);
    } catch (err) {
      this.processError(err);
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  private async assign(video: VideoWithEntryAndVisibility): Promise<void> {
    if (video.songEntry == null) {
      return;
    }
    this.assigning = true;
    try {
      await api.assignTag({ tags: this.tagInfo, songId: video.songEntry.id });
      video.processed = true;
    } catch (err) {
      this.processError(err);
    } finally {
      this.assigning = false;
    }
  }

  private async assignMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      for (const video of this.videos.filter(vid => vid.toAssign)) {
        await this.assign(video);
        video.toAssign = false;
      }
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
    }
  }

  private loadInitialPage(): void {
    this.updateUrl();
    this.fetch(this.tagName, this.scopeTagString, 0, 1);
  }

  private loadPage(page: number): void {
    this.updateUrl();
    this.fetch(
      this.tagNameFrozen,
      this.scopeTagStringFrozen,
      (page - 1) * this.maxResults,
      page
    );
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

  // session
  created(): void {
    let max_results = localStorage.getItem("max_results");
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
    let sort_by = localStorage.getItem("sort_by");
    if (sort_by != null) {
      this.sortingCondition = sort_by;
    }
    let nicovideo_mapped_tag = localStorage.getItem("nicovideo_mapped_tag");
    if (nicovideo_mapped_tag != null) {
      this.tagName = nicovideo_mapped_tag;
    }
    let nicovideo_mapped_tag_scope = localStorage.getItem(
      "nicovideo_mapped_tag_scope"
    );
    if (nicovideo_mapped_tag_scope != null) {
      this.scopeTagString = nicovideo_mapped_tag_scope;
    }
    let dbAddress = localStorage.getItem("dbAddress");
    if (this.dbAddress == "" && dbAddress != null) {
      this.dbAddress = dbAddress;
    }
  }

  // fill tag name from address params (override local storage)
  mounted(): void {
    let targName = this.$route.params["targName"];
    if (targName != undefined) {
      this.tagName = targName;
    }
  }
}
</script>
