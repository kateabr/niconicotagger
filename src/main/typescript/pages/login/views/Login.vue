<template>
  <div style="display: flex; align-items: center" class="min-vh-100 min-vw-100">
    <nav-bar-menu
      style="position: fixed; top: 0px"
      class="flex-lg-nowrap col-12"
      active-mode="index"
    />
    <div class="col-lg-3 mx-auto text-center">
      <h1>NicoNicoTagger</h1>
      <div class="blockquote">for VocaDB</div>
      <div class="container">
        <b-form class="justify-content-center" @submit="login">
          <b-form-group id="input-group-1" label-for="input-1">
            <b-form-input
              id="input-1"
              v-model="username"
              placeholder="Username"
              required
              @keydown.enter.native="login"
            ></b-form-input>
          </b-form-group>

          <b-form-group id="input-group-2" label-for="input-2">
            <b-form-input
              id="input-2"
              v-model="password"
              placeholder="Password"
              type="password"
              required
              @keydown.enter.native="login"
            ></b-form-input>
          </b-form-group>
          <b-button :disabled="loggingIn" block variant="primary" @click="login"
            >Login</b-button
          >
          <b-toast
            id="error"
            title="Error"
            no-auto-hide
            variant="danger"
            class="m-0 rounded-0"
            toaster="toaster"
          >
            {{ alertMessage }}
          </b-toast>
        </b-form>
      </div>
      <b-toaster class="b-toaster-bottom-center" name="toaster"> </b-toaster>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import { api } from "@/backend";
import NavBarMenu from "@/components/NavBarMenu.vue";

@Component({ components: { NavBarMenu } })
export default class extends Vue {
  private username: string = "";
  private password: string = "";
  private loggingIn: boolean = false;
  private alertMessage: string = "";

  private async login() {
    this.loggingIn = true;
    try {
      let response = await api.authenticate({
        username: this.username,
        password: this.password,
        database: "VocaDb"
      });
      localStorage.setItem("accessToken", response.data.token);
      window.location.href = "/tags";
    } catch (err) {
      this.$bvToast.show("error");
      this.alertMessage = err.response.data.message;
    } finally {
      this.loggingIn = false;
    }
  }
}
</script>
