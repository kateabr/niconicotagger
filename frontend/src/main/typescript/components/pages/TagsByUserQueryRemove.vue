<template>
  <b-row>
    <error-message
      :alert-code="alertCode"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <div
      class="py-lg-3 px-lg-4 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-row>
        <b-col class="my-auto">
          <b-input-group>
            <template #prepend>
              <b-input-group-text>{{ queryBase }} </b-input-group-text>
            </template>
            <b-input
              v-model="userQuery"
              placeholder="Other query params"
            /> </b-input-group
        ></b-col>
      </b-row>
      <b-row>
        <b-col class="my-auto">
          <b-button
            class="mt-2"
            block
            variant="primary"
            :href="queryBase + userQuery"
            target="_blank"
            :disabled="defaultDisableCondition()"
            >Preview<font-awesome-icon class="ml-1" icon="fas fa-external-link"
          /></b-button>
        </b-col>
        <b-col class="my-auto">
          <b-button
            class="mt-2"
            block
            variant="primary"
            :disabled="defaultDisableCondition()"
            @click="fetch()"
            ><span v-if="!songsInfoLoaded() && !fetching">Load</span
            ><b-spinner v-else-if="fetching" small /><span v-else
              >Reload</span
            ></b-button
          >
        </b-col>
        <b-col class="my-auto">
          <b-button
            v-b-toggle="'cheatsheet'"
            class="mt-2"
            block
            variant="primary"
            :disabled="defaultDisableCondition()"
            ><font-awesome-icon
              class="mr-sm-1"
              icon="fas fa-angle-down"
            />About</b-button
          >
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <b-collapse :id="'cheatsheet'" v-model="showCheatsheet" class="mt-2">
            <b-card class="text-left">
              <b-card-title>About</b-card-title>
              <b-card-text style="color: black"
                >This mode allows browsing and filtering songs in the same
                flexible way as the
                <b-link :href="dbAddress + '/Search?searchType=Song'"
                  >songs</b-link
                >
                and
                <b-link :href="dbAddress + '/Search?searchType=Artist'"
                  >artists</b-link
                >
                pages do, except that the search query needs to be written
                manually.<br />
                Most common query segments are listed below, everything else can
                be extracted from the database search page's address bar or
                VocaDB's source code.<br />
                Clicking the "Preview" button will open a new tab and show you
                the results you will get with your current query, so you can
                debug it until it does what you need.
                <div class="alert-info rounded p-2 mt-2">
                  <b-icon-exclamation-circle class="mr-1" />Remember that tag
                  removal can't be undone, please be cautious & conscious :)
                </div></b-card-text
              >
              <b-card-title>Basic query cheatsheet</b-card-title>
              <b-table-simple small>
                <tbody>
                  <tr>
                    <td class="text-monospace">getTotalCount</td>
                    <td class="text-monospace">true / false</td>
                    <td>
                      Whether to get the number of results. It is convenient, so
                      it is set to <span class="text-monospace">true</span> by
                      default.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">start</td>
                    <td class="">number</td>
                    <td>
                      How many results to skip in the beginning. Defaults to
                      <span class="text-monospace">0</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">maxResults</td>
                    <td class="text-monospace">number</td>
                    <td>
                      How many results you want in your batch, defaults to
                      <span class="text-monospace">10</span>. No need to make it
                      too big: it will slow everything down.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">sort</td>
                    <td class="text-monospace">
                      Name / AdditionDate / FavoritedTimes / RatingScore
                    </td>
                    <td>
                      Ordering condition. Can only use one, defaults to
                      <span class="text-monospace">None</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">lang</td>
                    <td class="text-monospace">Japanese / Romaji / English</td>
                    <td>
                      Name language. Defaults to
                      <span class="text-monospace">Default</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">tagId[]</td>
                    <td class="text-monospace">number</td>
                    <td>
                      Load only entries tagged with a specific tag. If you need
                      several tags, use several of these.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">childTags</td>
                    <td class="text-monospace">true / false</td>
                    <td>
                      Also load entries tagged by a chosen tag's descendants.
                      Defaults to <span class="text-monospace">false</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">status</td>
                    <td class="text-monospace">
                      Draft / Finished / Approved / Locked
                    </td>
                    <td>
                      Status restriction, off by default. Can only use one.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      songTypes<b-badge class="ml-1" variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">
                      Original, Remaster, Remix, Cover, Instrumental, Mashup,
                      MusicPV, DramaPV, Other
                    </td>
                    <td>
                      Song type restriction, defaults to
                      <span class="text-monospace">Unspecified</span> (no
                      restrictions). Separate with comma.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      artistId[]<b-badge class="ml-1" variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">number</td>
                    <td>
                      Load only songs by a select artist. Use several of these
                      for several artists.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      minScore<b-badge class="ml-1" variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">number</td>
                    <td>
                      Rating threshold, defaults to
                      <span class="text-monospace">0</span>.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      dateYear, dateMonth, dateDay<b-badge
                        class="ml-1"
                        variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">numbers</td>
                    <td>
                      Specific publish date. Can be used together or each on
                      their own.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      parentVersionId<b-badge class="ml-1" variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Id of the songs' original version.</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      minLength, maxLength<b-badge
                        class="ml-1"
                        variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Length restriction (in seconds).</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      eventId<b-badge class="ml-1" variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Id of the songs' release event.</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      onlyWithPVs<b-badge class="ml-1" variant="primary"
                        >songs</b-badge
                      >
                    </td>
                    <td class="text-monospace">true / false</td>
                    <td>
                      Defaults to <span class="text-monospace">false.</span>
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      artistTypes<b-badge class="ml-1" variant="success"
                        >artists</b-badge
                      >
                    </td>
                    <td class="text-monospace">
                      Circle, Label, Producer, Animator, Illustrator, Lyricist,
                      Vocaloid, UTAU, CeVIO, OtherVoiceSynthesizer,
                      OtherVocalist, OtherGroup, SynthesizerV, CoverArtist
                    </td>
                    <td>
                      Artist type restriction, defaults to
                      <span class="text-monospace">Unspecified</span> (no
                      restrictions). Separate with comma.
                    </td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      followedByUserId<b-badge class="ml-1" variant="success"
                        >artists</b-badge
                      >
                    </td>
                    <td class="text-monospace">number</td>
                    <td>Only load artists followed by a specific user.</td>
                  </tr>
                  <tr>
                    <td class="text-monospace">
                      fields<b-badge class="ml-1" variant="dark"
                        >not used</b-badge
                      >
                    </td>
                    <td class="text-monospace">-</td>
                    <td>
                      This parameter specifies which extra fields you would like
                      to get along with the basic entry. Here it is already set
                      to <span class="text-monospace">Tags</span>, so its
                      subsequent occurrences will be ignored.
                    </td>
                  </tr>
                </tbody>
              </b-table-simple>
              <b-card-title>Advanced filters</b-card-title>
              <b-card-text style="color: black"
                >These allow for a more fine-tuned search but also take more
                time to process.<br />
                <span style="text-decoration: underline"
                  >Please note that this feature does not come out of the box,
                  so it is possible that not every combination of conditions is
                  fully implemented and this is not an error.</span
                ><br />
                <p>
                  Each advanced filter parameter has the following structure:
                </p></b-card-text
              >
              <blockquote style="color: black" class="ml-3 text-monospace">
                advancedFilters[number][type / param / negate]=value
              </blockquote>
              <b-card-text style="color: black"
                >Here are some filter types:
                <ul>
                  <li>
                    ArtistType<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    HasAlbum<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    HasMedia<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    HasOriginalMedia<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    HasPublishDate<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    Lyrics<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    LyricsContent<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    WebLink<b-badge class="ml-1" variant="primary"
                      >songs</b-badge
                    >
                  </li>
                  <li>
                    VoiceProvider<b-badge class="ml-1" variant="success"
                      >artists</b-badge
                    >
                  </li>
                  <li>
                    RootVoicebank<b-badge class="ml-1" variant="success"
                      >artists</b-badge
                    >
                  </li>
                  <li>
                    HasUserAccount<b-badge class="ml-1" variant="success"
                      >artists</b-badge
                    >
                  </li>
                </ul>
                <span class="text-monospace">Filter param</span> is the value
                you need: you can specify one or several exact values or use a
                wildcard (<span class="text-monospace">*</span>) to select all
                possible values. <span class="text-monospace">Negate</span> is
                used to invert the condition.</b-card-text
              >
              <b-card-text style="color: black"
                ><p>
                  <strong>Example:</strong> Filter for all songs that do not
                  feature UTAU and have lyrics in any language
                </p>
                <blockquote style="color: black" class="ml-3 text-monospace">
                  advancedFilters[0][filterType]=ArtistType&advancedFilters[0][negate]=true&advancedFilters[0][param]=UTAU&advancedFilters[1][filterType]=Lyrics&advancedFilters[1][param]=*
                </blockquote></b-card-text
              >
            </b-card>
          </b-collapse>
        </b-col>
      </b-row>
      <b-row v-if="tagPool.length > 0" cols="12" class="mt-2">
        <b-col cols="12" class="my-auto mx-auto">
          <b-card class="text-left" title="Tags to remove from all entries">
            <b-card-text v-if="tagsToRemove.length > 0">
              <b-button
                v-for="tag in tagsToRemove"
                :key="tag.id"
                class="m-1"
                :disabled="defaultDisableCondition()"
                size="sm"
                variant="danger"
                @click="removeTagIdFromAllTaggedSongs(tag.id)"
              >
                <font-awesome-icon icon="fa-solid fa-xmark" class="mr-1" />{{
                  tag.name
                }}
              </b-button>
            </b-card-text>
            <b-card-text v-else class="text-muted">None selected</b-card-text>
            <vue-bootstrap-typeahead
              v-model="tempTagName"
              size="sm"
              prepend="Add a tag"
              :serializer="t => t.name"
              :data="tagPool"
              @hit="addCommonTagToRemove($event)"
            />
          </b-card>
        </b-col>
      </b-row>
    </div>
    <div class="py-lg-3 px-lg-4 col-lg-12 text-left m-auto">
      <b-row
        v-if="totalVideoCount >= entries.length && songsInfoLoaded()"
        cols="12"
      >
        <b-col
          >Query yielded {{ totalVideoCount }} results (displaying
          {{ entries.length }}):</b-col
        >
      </b-row>
      <b-row v-else-if="songsInfoLoaded() && entries.length === 0"
        >Query yielded no results.</b-row
      >
    </div>
    <b-table-simple
      v-if="songsInfoLoaded() && isActiveMode()"
      hover
      class="mt-1 col-lg-12"
    >
      <b-thead>
        <b-th>
          <b-form-checkbox class="invisible" size="lg"></b-form-checkbox>
        </b-th>
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Tags</b-th>
      </b-thead>
      <b-tbody v-if="entries.length > 0">
        <tr v-for="entry in entries" :key="entry.item.id">
          <td>
            <b-form-checkbox
              v-if="entry.toRemove"
              :checked="entry.toRemove"
              size="lg"
              :disabled="defaultDisableCondition()"
              @change="checkboxClicked(entry)"
            />
          </td>
          <td>
            <title-displayer
              :elem-type="removalMode"
              :elem-name="entry.item.name"
              :elem-link="getEntryUrl(entry.item.id)"
              :elem-type-abbr="getShortenedType(entry)"
              :elem-type-color="getTypeColorForDisplay(entry)"
              :artist-string="
                removalMode === 'songs' ? entry.item.artistString : ''
              "
            />
          </td>
          <td>
            <span v-for="tag in entry.item.tags" :key="tag.id">
              <b-button
                size="sm"
                :variant="getTagVariant(tag.id, entry.tagIdsForRemoval)"
                class="m-1"
                :disabled="defaultDisableCondition()"
                @click="toggleTagRemoval(tag.id, entry)"
              >
                <font-awesome-icon
                  :icon="getTagIcon(tag.id, entry.tagIdsForRemoval)"
                  class="sm mr-sm-1"
                />{{ tag.name }}</b-button
              >
            </span>
          </td>
        </tr>
      </b-tbody>
      <b-tbody v-else>
        <b-tr>
          <b-td colspan="4" class="text-center text-muted">
            <small>No items to display</small>
          </b-td>
        </b-tr>
      </b-tbody>
      <b-tfoot>
        <b-th></b-th>
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Tags</b-th>
      </b-tfoot>
    </b-table-simple>
    <b-row
      v-if="songsInfoLoaded()"
      class="mt-lg-1 col-lg-12 mb-5 text-center mx-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="col-lg-3 m-auto">
        <b-button
          block
          variant="primary"
          :disabled="countChecked() === 0 || executing"
          @click="removeTags"
        >
          <div v-if="executing">
            <b-spinner small class="mr-1"></b-spinner>
            Removing tags...
          </div>
          <div v-else>
            Remove tags from songs ({{ countChecked() }} selected)
          </div>
        </b-button>
      </b-col>
    </b-row>
  </b-row>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import {
  getSongTypeColorForDisplay,
  getVocaDBEntryUrl,
  getShortenedSongType,
  getVocaDBArtistUrl,
  getShortenedArtistType,
  getArtistTypeColorForDisplay
} from "@/utils";
import { api } from "@/backend";
import {
  MinimalTag,
  EntryForTagRemoval,
  SongForApiContractSimplified,
  ArtistForApiContractSimplified
} from "@/backend/dto";
import NicoEmbed from "@/components/NicoEmbed.vue";
import ProgressBar from "@/components/ProgressBar.vue";
import ErrorMessage from "@/components/ErrorMessage.vue";
import TitleDisplayer from "@/components/TitleDisplayer.vue";
import { AxiosResponse } from "axios";

@Component({
  components: { TitleDisplayer, NicoEmbed, ProgressBar, ErrorMessage }
})
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  @Prop()
  private readonly userQueryProp!: string;

  @Prop()
  private readonly removalMode!: "songs" | "artists";

  // main variables
  private entries: EntryForTagRemoval[] = [];
  private tagPool: MinimalTag[] = [];
  private tagsToRemove: MinimalTag[] = [];
  private tempTagName: string = "";

  // api variables
  private dbAddress: string = "";
  private queryBase: string = "/api/" + this.removalMode + "?fields=Tags&";
  private userQuery: string = "getTotalCount=true";

  // interface variables
  private executing: boolean = false;
  private fetching: boolean = false;
  private totalVideoCount: number = 0;
  private loaded: boolean = false;
  private showCheatsheet: boolean = false;

  // error handling
  private alertCode: number = 0;
  private alertMessage: string = "";

  //proxy methods
  private getTagVariant(tagId: number, tagIdsForRemoval: number[]): string {
    if (tagIdsForRemoval.find(t => t == tagId) != undefined) {
      return "danger";
    } else {
      return "outline-success";
    }
  }

  private getTagIcon(tagId: number, tagsIdsForRemoval: number[]): string[] {
    if (tagsIdsForRemoval.find(t => t == tagId) != undefined) {
      return ["fas", "fa-xmark"];
    } else {
      return ["fas", "fa-check"];
    }
  }

  private getShortenedType(entry: EntryForTagRemoval): string {
    if (this.removalMode == "songs") {
      let typeString = (entry.item as SongForApiContractSimplified).songType;
      return getShortenedSongType(typeString);
    } else if (this.removalMode == "artists") {
      let typeString = (entry.item as ArtistForApiContractSimplified)
        .artistType;
      return getShortenedArtistType(typeString);
    } else {
      return "Invalid removal mode: " + this.removalMode;
    }
  }

  private getEntryUrl(id: number): string {
    if (this.removalMode == "songs") {
      return getVocaDBEntryUrl(this.dbAddress, id);
    } else if (this.removalMode == "artists") {
      return getVocaDBArtistUrl(this.dbAddress, id);
    } else {
      return "Invalid removal mode: " + this.removalMode;
    }
  }

  private getTypeColorForDisplay(entry: EntryForTagRemoval): string {
    if (this.removalMode == "songs") {
      return getSongTypeColorForDisplay(
        (entry.item as SongForApiContractSimplified).songType
      );
    } else if (this.removalMode == "artists") {
      return getArtistTypeColorForDisplay(
        (entry.item as ArtistForApiContractSimplified).artistType
      );
    } else {
      return "Invalid removal mode: " + this.removalMode;
    }
  }

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.executing;
  }

  private songsInfoLoaded(): boolean {
    return this.loaded;
  }

  private countChecked(): number {
    return this.entries.filter(video => video.toRemove).length;
  }

  private toggleTagRemoval(tagId: number, video: EntryForTagRemoval): void {
    if (video.tagIdsForRemoval.find(t => t == tagId) != undefined) {
      video.tagIdsForRemoval = video.tagIdsForRemoval.filter(t => t != tagId);
    } else {
      video.tagIdsForRemoval.push(tagId);
    }
    video.toRemove = video.tagIdsForRemoval.length > 0;
  }

  private addTagIdToAllTaggedSongs(tagId: number): void {
    for (const video of this.entries) {
      if (video.item.tags.map(t => t.id).find(t => t == tagId) != undefined) {
        video.tagIdsForRemoval.push(tagId);
        video.toRemove = true;
      }
    }
  }

  private removeTagIdFromAllTaggedSongs(tagId: number): void {
    for (const video of this.entries) {
      video.tagIdsForRemoval = video.tagIdsForRemoval.filter(t => t != tagId);
      video.toRemove = video.tagIdsForRemoval.length > 0;
    }
    this.tagsToRemove = this.tagsToRemove.filter(tag => tag.id != tagId);
  }

  private checkboxClicked(video: EntryForTagRemoval): void {
    video.toRemove = false;
    video.tagIdsForRemoval = [];
  }

  // for tag autocomplete
  private addCommonTagToRemove(tag: MinimalTag): void {
    this.addTagIdToAllTaggedSongs(tag.id);
    if (this.tagsToRemove.find(t => t.id == tag.id) == undefined) {
      this.tagsToRemove.push(tag);
    }
    return;
  }

  private updateUrl(): void {
    this.$router
      .push({
        name: "console-full",
        params: { browseMode: this.thisMode, userQuery: this.userQuery }
      })
      .catch(err => {
        return false;
      });
  }

  // api methods
  async fetch(): Promise<void> {
    this.updateUrl();
    this.tagsToRemove = [];
    this.fetching = true;
    this.showCheatsheet = false;
    try {
      let response;
      if (this.removalMode == "songs") {
        response = await api.fetchSongsForTagRemoval({
          query: this.userQuery,
          dbAddress: this.dbAddress
        });
      } else if (this.removalMode == "artists") {
        response = await api.fetchArtistsForTagRemoval({
          query: this.userQuery,
          dbAddress: this.dbAddress
        });
      } else {
        throw {
          response: undefined,
          message: "Unknown removal mode: " + this.removalMode
        };
      }
      this.entries = response.items.map(item => {
        return {
          item: item.item,
          toRemove: item.toRemove,
          tagIdsForRemoval: []
        };
      });
      this.tagPool = response.tagPool;
      this.totalVideoCount = response.totalCount;
    } catch (err) {
      this.processError(err);
    } finally {
      this.loaded = true;
      this.fetching = false;
    }
  }

  private async removeTags(): Promise<void> {
    this.executing = true;
    let entriesToRemove = this.entries.filter(s => s.toRemove);
    this.showCheatsheet = false;
    try {
      for (const entry of entriesToRemove) {
        await api.removeTags({
          id: entry.item.id,
          mode: this.removalMode,
          tagIds: entry.tagIdsForRemoval
        });
        entry.item.tags = entry.item.tags.filter(
          t => entry.tagIdsForRemoval.find(tagId => tagId == t.id) == undefined
        );
        for (const tagId of entry.tagIdsForRemoval) {
          if (this.tagsToRemove.find(t => t.id == tagId) == undefined) {
            let removeFromPool = true;
            for (const video of this.entries) {
              if (
                video.item.id != entry.item.id &&
                video.item.tags.find(t => t.id == tagId) != undefined
              ) {
                removeFromPool = false;
                break;
              }
            }
            if (removeFromPool) {
              this.tagPool = this.tagPool.filter(t => t.id != tagId);
            }
          }
        }
        entry.tagIdsForRemoval = [];
        entry.toRemove = false;
      }
    } catch (err) {
      this.processError(err);
    } finally {
      for (const tag of this.tagsToRemove) {
        let removeFromPool = true;
        for (const video of this.entries) {
          if (video.item.tags.filter(t => t.id == tag.id).length != 0) {
            removeFromPool = false;
            break;
          }
        }
        if (removeFromPool) {
          this.tagPool = this.tagPool.filter(t => t.id != tag.id);
        }
      }
      this.tagsToRemove = [];
      this.executing = false;
    }
  }

  // error handling
  private processError(
    err: { response: AxiosResponse } | { response: undefined; message: string }
  ): void {
    this.$bvToast.show("error_" + this.thisMode);
    if (err.response == undefined) {
      this.alertCode = 0;
      this.alertMessage = err.message;
    } else {
      this.alertCode = err.response.data.code;
      let stacktrace = err?.response?.data?.stacktrace ?? "";
      this.alertMessage =
        err.response.data.message + stacktrace != undefined
          ? " (" + stacktrace[0] + ")"
          : "";
    }
  }

  // session
  created(): void {
    let dbAddress = localStorage.getItem("dbAddress");
    if (this.dbAddress == "" && dbAddress != null) {
      this.dbAddress = dbAddress;
      this.queryBase = dbAddress + this.queryBase;
    }
  }

  // fill userQuery name from address params
  mounted(): void {
    let userQuery = this.$route.params["userQuery"];
    if (userQuery != undefined) {
      this.userQuery = userQuery;
    }
  }
}
</script>
