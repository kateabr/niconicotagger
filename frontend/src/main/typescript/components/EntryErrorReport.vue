<template>
  <div>
    <div v-if="errorReport != null" class="alert-danger rounded p-2 mt-1">
      <b-icon-exclamation-circle class="mr-1" />
      <span v-if="errorReport.message != null">{{ errorReport.message }}</span>
      <span v-else>Unknown error, could not update</span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

import { UpdateErrorReport } from "@/backend/dto/lowerLevelStruct";

@Component({ components: {} })
export default class extends Vue {
  @Prop()
  private readonly errorReport!: UpdateErrorReport | null;
}
</script>
