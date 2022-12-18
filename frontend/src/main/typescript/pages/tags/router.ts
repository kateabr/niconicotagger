import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/:browseMode/:targName",
    name: "tags-full",
    props: true,
    component: () => import("@/pages/tags/views/Tags.vue")
  },
  {
    path: "/:browseMode",
    name: "tags-mode",
    props: true,
    component: () => import("@/pages/tags/views/Tags.vue")
  },
  {
    path: "/",
    name: "tags",
    props: {
      browseMode: "activity-entries"
    },
    component: () => import("@/pages/tags/views/Tags.vue")
  }
];

export const router = new VueRouter({
  base: "/tags",
  mode: "history",
  routes: routes
});
