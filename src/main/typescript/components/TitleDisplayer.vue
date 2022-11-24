<template>
  <span>
    <b-link target="_blank" :href="elemLink"
      >{{ elemName
      }}<b-badge
        v-if="elemTypeAbbr !== ''"
        class="badge text-center ml-2"
        :variant="elemTypeColor"
      >
        {{ elemTypeAbbr }}
      </b-badge>
    </b-link>
    <span v-if="elemType === 'songs'" class="text-muted">
      <div>
        {{ artistString }}
      </div>
    </span>
    <span v-else-if="elemType === 'artists'"> </span>
    <span v-else>Unknown element type: {{ elemType }}!</span>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly elemName!: string;

  @Prop()
  private readonly elemLink!: string;

  @Prop()
  private readonly elemTypeColor!: string;

  @Prop()
  private readonly elemTypeAbbr!: string;

  @Prop()
  private readonly elemType!: "songs" | "artists";

  @Prop()
  private readonly artistString!: string;
}
</script>
