<template>
  <div style="display: flex; align-items: center">
    <b-container class="col-lg-11">
      <b-row>
        <b-toaster class="b-toaster-top-center" name="toaster-2"></b-toaster>
        <span class="m-auto col-lg-5">
          <b-input-group inline class="mt-lg-3">
            <template #prepend>
              <b-button
                v-b-toggle.scope-collapse
                variant="primary"
                style="width: 80px"
                :disabled="defaultDisableCondition()"
                ><i class="fas fa-angle-down mr-sm-1"></i>More</b-button
              >
            </template>
            <b-form-input
              id="tag-form"
              v-model.trim="tag"
              :disabled="defaultDisableCondition()"
              placeholder="Tag"
              @keydown.enter.native="fetch(tag, 0, 1)"
            >
            </b-form-input>
            <template #append>
              <b-button
                v-if="!fetching"
                variant="primary"
                style="width: 80px"
                :disabled="tag === '' || defaultDisableCondition()"
                @click="fetch(tag.trim(), 0, 1)"
                >Load</b-button
              >
              <b-button v-else variant="primary" style="width: 80px" disabled
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
                    v-model="scopeTag"
                    placeholder="Specify tag scope"
                    :disabled="defaultDisableCondition()"
                    @keydown.enter.native="fetch(tag.trim(), 0, 1)"
                  >
                  </b-form-input>
                  <template #prepend>
                    <b-button
                      variant="secondary"
                      style="width: 80px"
                      @click="
                        scopeTag =
                          '-歌ってみた VOCALOID OR UTAU OR CEVIO OR SYNTHV OR SYNTHESIZERV OR neutrino(歌声合成エンジン) OR DeepVocal OR Alter/Ego OR AlterEgo OR AquesTalk OR AquesTone OR AquesTone2 OR ボカロ OR ボーカロイド OR 合成音声 OR 歌唱合成 OR coefont OR coefont_studio OR VOICELOID OR VOICEROID'
                      "
                    >
                      <i class="fas fa-paste"></i>
                    </b-button>
                  </template>
                  <template #append>
                    <b-button
                      variant="danger"
                      style="width: 80px"
                      :disabled="scopeTag === ''"
                      @click="scopeTag = ''"
                      >Clear</b-button
                    >
                  </template>
                </b-input-group>
              </b-col>
            </b-row>
            <b-row v-if="tagFrozen !== ''" class="mt-2">
              <b-col>
                <b-dropdown
                  block
                  :disabled="defaultDisableCondition()"
                  :text="getOrderingCondition()"
                  variant="primary"
                >
                  <b-dropdown-item
                    v-for="(key, value) in orderOptions"
                    :key="key"
                    :disabled="orderBy === value"
                    @click="setOrderBy(value)"
                  >
                    {{ orderOptions[value] }}
                  </b-dropdown-item>
                </b-dropdown>
              </b-col>
              <b-col>
                <template>
                  <b-input-group
                    inline
                    :state="pageState"
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
                        :state="pageState()"
                        @keydown.enter.native="
                          pageState()
                            ? fetch(
                                tagFrozen,
                                (pageToJump - 1) * maxResults,
                                pageToJump
                              )
                            : null
                        "
                      >
                      </b-form-input>
                    </template>
                    <template #append>
                      <b-button
                        style="width: 80px"
                        :variant="pageState() ? 'success' : 'danger'"
                        :disabled="defaultDisableCondition() || !pageState()"
                        @click="
                          fetch(
                            tagFrozen,
                            (pageToJump - 1) * maxResults,
                            pageToJump
                          )
                        "
                        ><span v-if="pageToJump === page">Refresh</span
                        ><span v-else>Jump</span></b-button
                      >
                    </template>
                  </b-input-group>
                </template>
              </b-col>
            </b-row>
          </b-collapse>
        </span>
        <b-toast
          id="error"
          title="Error"
          no-auto-hide
          variant="danger"
          class="m-0 rounded-0"
          toaster="toaster-2"
        >
          {{ alertMessage }}
        </b-toast>
      </b-row>
      <b-row
        v-if="tagInfo.length !== 0"
        class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
      >
        <b-col class="my-auto"
          >Tag:<br /><strong>
            <a
              v-if="tagInfo.length !== 0"
              target="_blank"
              :href="getNicoTag(tagFrozen, scopeTagFrozen !== '')"
              >{{ tagFrozen }}</a
            >
          </strong></b-col
        >
        <b-col class="my-auto"
          >Mapped to:<br />
          <strong v-for="(tag, key) in tagMappings" :key="key">
            <a target="_blank" :href="getVocaDBTag(tagInfo[key])">{{ tag }}</a
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
            @click="filter"
            >Videos without entries
          </b-button>
        </b-col>
        <b-col class="my-auto">
          <b-button
            :disabled="defaultDisableCondition()"
            variant="primary"
            block
            :pressed.sync="tagged"
            @click="filter"
            >Tagged songs
          </b-button>
        </b-col>
      </b-row>
      <b-row v-if="tagInfo.length > 0">
        <b-col class="col-12">
          <div class="text-center pt-sm-3">
            <b-button-group>
              <b-button
                v-for="(type, key) in songTypes"
                :key="key"
                class="pl-4 pr-4"
                :disabled="defaultDisableCondition()"
                :variant="
                  (type.show ? '' : 'outline-') + getSongTypeVariant(type.name)
                "
                @click="
                  type.show = !type.show;
                  filter();
                "
                >{{ getTypeInfo(type.name) }}
              </b-button>
            </b-button-group>
          </div>
        </b-col>
      </b-row>
      <b-row v-if="tagInfo.length !== 0">
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
              @change="pageClicked"
            ></b-pagination>
          </div>
        </template>
      </b-row>
      <b-row v-if="tagInfo.length !== 0">
        <b-table-simple
          hover
          :disabled="defaultDisableCondition()"
          class="mt-1 col-lg-12"
        >
          <b-thead>
            <b-th>
              <b-form-checkbox
                v-model="allChecked"
                size="lg"
                :disabled="defaultDisableCondition()"
                @change="checkAll"
              ></b-form-checkbox>
            </b-th>
            <b-th class="col-8 align-middle">Video</b-th>
            <b-th class="col-3 align-middle">Entry</b-th>
            <b-th class="col-1 align-middle">Tag</b-th>
          </b-thead>
          <b-tbody v-if="videosToDisplay.length > 0">
            <b-tr
              v-for="(value, key) in videosToDisplay"
              :id="value.video.contentId"
              :key="key"
            >
              <b-td>
                <div
                  v-if="value.songEntry != null && !value.songEntry.tagInTags"
                >
                  <b-form-checkbox
                    v-model="value.toAssign"
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
                  @click="value.visible = !value.visible"
                >
                  <i class="fas fa-play"></i
                ></b-button>
                <a
                  target="_blank"
                  :href="getVideoUrl(value.video)"
                  v-html="value.video.title"
                ></a>
                <div>
                  <b-badge
                    v-for="(value1, key1) in value.video.tags"
                    :key="key1"
                    v-clipboard:copy="value1.name"
                    class="m-sm-1"
                    :variant="value1.variant"
                    href="#"
                    ><i class="fas fa-tag"></i> {{ value1.name }}
                  </b-badge>
                </div>
                <b-collapse
                  :id="getCollapseId(value.video.contentId)"
                  :visible="value.visible && !fetching"
                  class="mt-2 collapsed"
                >
                  <b-card
                    v-cloak
                    :id="'embed_' + value.video.contentId"
                    class="embed-responsive embed-responsive-16by9"
                  >
                    <iframe
                      v-if="value.visible && !fetching"
                      class="embed-responsive-item"
                      allowfullscreen="allowfullscreen"
                      style="border: none"
                      :src="getEmbedAddr(value.video.contentId)"
                    ></iframe>
                  </b-card>
                </b-collapse>
              </b-td>
              <b-td>
                <div v-if="value.songEntry != null">
                  <a
                    target="_blank"
                    :href="getEntryUrl(value.songEntry)"
                    v-html="value.songEntry.name"
                  ></a>
                  <a target="_blank" :href="getEntryUrl(value.songEntry)">
                    <b-badge
                      class="badge text-center ml-2"
                      :variant="getSongTypeVariant(value.songEntry.songType)"
                    >
                      {{ getSongType(value.songEntry.songType) }}
                    </b-badge>
                  </a>
                  <div class="text-muted">
                    {{ value.songEntry.artistString }}
                  </div>
                </div>
                <div v-else>
                  <b-button
                    size="sm"
                    :disabled="fetching"
                    :href="getAddSongUrl(value.video.contentId)"
                    target="_blank"
                    >Add to the database
                  </b-button>
                </div>
              </b-td>
              <b-td>
                <div v-if="value.songEntry != null">
                  <b-button-toolbar key-nav>
                    <b-button
                      v-if="value.songEntry.tagInTags"
                      style="pointer-events: none"
                      class="btn disabled"
                      variant="success"
                    >
                      <i class="fas fa-check"></i>
                    </b-button>
                    <b-button
                      v-else
                      :id="getButtonId(value.songEntry)"
                      :disabled="defaultDisableCondition()"
                      class="btn"
                      variant="outline-success"
                      @click="assign(value.songEntry.id)"
                    >
                      <i class="fas fa-plus"></i>
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
                :disabled="defaultDisableCondition()"
                @change="checkAll"
              ></b-form-checkbox>
            </b-th>
            <b-th class="col-8 align-middle">Video</b-th>
            <b-th class="col-3 align-middle">Entry</b-th>
            <b-th class="col-1 align-middle">Tag</b-th>
          </b-tfoot>
        </b-table-simple>
      </b-row>
      <b-row
        v-if="tagInfo.length !== 0"
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
      <b-row v-if="tagInfo.length !== 0">
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
            @change="pageClicked"
          ></b-pagination>
        </div>
      </b-row>
    </b-container>
    <b-row>
      <b-col
        ><b-link to="vocadb"
          ><b-button size="sm" variant="dark" class="fixed-top m-1" squared
            >Toggle<br />mode</b-button
          ></b-link
        ></b-col
      >
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import {
  AssignableTag,
  NicoVideo,
  NicoVideoWithTidyTags,
  SongForApiContractSimplified
} from "@/backend/dto";
import { api } from "@/backend";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({ components: {} })
export default class extends Vue {
  private tag: string = "";
  private orderBy = "startTime";
  private orderOptions = {
    startTime: "upload time",
    viewCounter: "views",
    lengthSeconds: "length"
  };
  private tagFrozen: string = "";
  private tagInfo: AssignableTag[] = [];
  private startOffset: number = 0;
  private maxResults: number = 10;
  private tagMappings: string[] = [];
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
  private videos: VideoWithEntryAndVisibility[] = [];
  private videosToDisplay: VideoWithEntryAndVisibility[] = [];
  private totalVideoCount: number = 0;
  private fetching: boolean = false;
  private showTable: boolean = false;
  private noEntry: boolean = true;
  private tagged: boolean = true;
  private page: number = 1;
  private numOfPages: number = 1;
  private allChecked: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;
  private alertMessage: string = "";
  private scopeTag: string = "";
  private scopeTagFrozen: string = "";
  private showCollapse: boolean = false;
  private pageToJump: number = this.page;
  private maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
  private fetchMode = ["database", "NND"];

  async fetch(
    targetTag: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    if (this.tag == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      let response = await api.fetchVideos({
        tag: targetTag,
        scopeTag: this.scopeTag,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.orderBy
      });
      this.videos = response.items.map(vid => {
        return {
          video: vid.video,
          songEntry: vid.songEntry,
          visible: false,
          toAssign: false
        };
      });
      this.filter();
      this.tagMappings = response.tagMappings;
      this.totalVideoCount = response.totalVideoCount;
      this.scopeTagFrozen = response.safeScope;
      this.scopeTag = response.safeScope;
      this.tagInfo = response.tags;
      this.tagFrozen = targetTag;
      this.tag = targetTag;
      this.showTable = this.videos.length > 0;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.allChecked = false;
    } catch (err) {
      this.$bvToast.show("error");
      this.alertMessage = err.response.data.message;
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  getEntryUrl(songEntry: SongForApiContractSimplified): string {
    return "https://vocadb.net/S/" + songEntry.id;
  }

  getVideoUrl(video: NicoVideo): string {
    return "https://nicovideo.jp/watch/" + video.contentId;
  }

  getResultNumberStr(): string {
    return "Videos per page: " + this.maxResults;
  }

  getNicoTag(tag: string, scoped: boolean): string {
    if (scoped) {
      return "https://nicovideo.jp/tag/" + this.scopeTagFrozen + " " + tag;
    }
    return "https://nicovideo.jp/tag/" + tag;
  }

  getVocaDBTag(tag: AssignableTag): string {
    return "https://vocadb.net/T/" + tag.id + "/" + tag.urlSlug;
  }

  pageClicked(pgnum: number): void {
    this.fetch(this.tagFrozen, (pgnum - 1) * this.maxResults, pgnum);
  }

  setMaxResults(mxres: number): void {
    this.maxResults = mxres;
    this.fetch(this.tagFrozen, 0, 1);
  }

  private async assign(id: number): Promise<void> {
    this.assigning = true;
    await api.assignTag({ tags: this.tagInfo, songId: id });
    let songEntry = this.videosToDisplay.filter(video => {
      if (video.songEntry == null) return false;
      return video.songEntry.id == id;
    })[0].songEntry as SongForApiContractSimplified;
    this.assigning = false;
    songEntry.tagInTags = true;
  }

  private async assignMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      for (const vid1 of this.videosToDisplay.filter(vid => vid.toAssign)) {
        let songEntry = vid1.songEntry as SongForApiContractSimplified;
        await this.assign(songEntry.id);
        vid1.toAssign = false;
      }
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
    }
  }

  getButtonId(song: SongForApiContractSimplified): string {
    return "assign_" + song.id;
  }

  getCollapseId(videoId: string): string {
    return "collapse_" + videoId;
  }

  getEmbedAddr(videoId: string): string {
    return (
      "https://embed.nicovideo.jp/watch/" +
      videoId +
      "?noRelatedVideo=1&enablejsapi=0"
    );
  }

  checkAll(): void {
    this.videosToDisplay
      .filter(video => video.songEntry != null && !video.songEntry.tagInTags)
      .forEach(video => (video.toAssign = this.allChecked));
  }

  countChecked(): number {
    return this.videosToDisplay.filter(video => video.toAssign).length;
  }

  getSongType(typeString: string): string {
    if (typeString == "Unspecified") {
      return "?";
    } else if (typeString == "MusicPV") {
      return "PV";
    } else {
      return typeString[0];
    }
  }

  getSongTypeVariant(typeString: string): string {
    if (typeString == "Original" || typeString == "Remaster") {
      return "primary";
    } else if (
      typeString == "Remix" ||
      typeString == "Cover" ||
      typeString == "Mashup" ||
      typeString == "Other"
    ) {
      return "secondary";
    } else if (typeString == "Instrumental") {
      return "dark";
    } else if (typeString == "MusicPV" || typeString == "DramaPV") {
      return "success";
    } else {
      return "warning";
    }
  }

  getAddSongUrl(pvLink: string): string {
    return (
      "https://vocadb.net/Song/Create?PVUrl=https://www.nicovideo.jp/watch/" +
      pvLink
    );
  }

  private setOrderBy(value: string): void {
    this.orderBy = value;
    this.fetch(this.tagFrozen, 0, 1);
  }

  private filter(): void {
    this.videosToDisplay = this.videos.filter(
      vid =>
        (vid.songEntry != null || this.noEntry) &&
        (vid.songEntry == null || this.tagged || !vid.songEntry.tagInTags)
    );
    if (this.hiddenTypes() > 0) {
      this.videosToDisplay = this.videosToDisplay.filter(vid => {
        if (vid.songEntry != null)
          return !this.songTypes
            .filter(t => !t.show)
            .map(t => t.name)
            .includes(vid.songEntry.songType);
        else return true;
      });
    }
  }

  pageState(): boolean {
    return this.pageToJump > 0 && this.pageToJump <= this.maxPage;
  }

  private hiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private getOrderingCondition(): string {
    return "Arrange videos by: " + this.orderOptions[this.orderBy];
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning || this.assigning;
  }

  private getTypeInfo(type: string): string {
    return (
      type +
      " (" +
      this.videos.filter(
        vid => vid.songEntry != null && vid.songEntry.songType == type
      ).length +
      ")"
    );
  }
}

export interface VideoWithEntryAndVisibility {
  video: NicoVideoWithTidyTags;
  songEntry: SongForApiContractSimplified | null;
  visible: boolean;
  toAssign: boolean;
}

export interface SongType {
  name: string;
  show: boolean;
}
</script>
