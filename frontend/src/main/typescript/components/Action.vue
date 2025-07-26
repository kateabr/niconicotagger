<template>
  <div>
    <ol>
      <li v-if="event != null">
        Add "<b-link
          :href="getVocaDBEventUrl(clientType, event.id)"
          target="_blank"
          >{{ event.name }}</b-link
        >" to release events
      </li>
      <li v-if="tagToRemove != null">
        Remove tag "<b-link
          :href="getVocaDBTagUrl(clientType, tagToRemove.id)"
          target="_blank"
          >{{ tagToRemove.name }}</b-link
        >"
      </li>
    </ol>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { getVocaDBEventUrl, getVocaDBTagUrl } from "@/utils";
import { ReleaseEvent, VocaDbTag } from "@/backend/dto/lowerLevelStruct";

@Component({
  methods: { getVocaDBTagUrl, getVocaDBEventUrl }
})
export default class extends Vue {
  @Prop()
  private readonly event!: ReleaseEvent | null;

  @Prop()
  private readonly tagToRemove!: VocaDbTag | null;

  @Prop()
  private readonly clientType!: string;
}
</script>
