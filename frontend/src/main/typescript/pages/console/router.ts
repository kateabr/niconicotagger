import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import { serviceName } from "@/constants";
import { BrowseMode, defaultMode } from "@/pages/console/utils";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/:browseMode/:userQuery",
    name: "console-full",
    props: true,
    component: () => import("@/pages/console/views/QueryConsole.vue")
  },
  {
    path: "/:browseMode",
    name: "console-mode",
    props: true,
    component: () => import("@/pages/console/views/QueryConsole.vue")
  },
  {
    path: "/",
    name: "console",
    props: {
      browseMode: defaultMode
    },
    component: () => import("@/pages/console/views/QueryConsole.vue")
  }
];

export const router = new VueRouter({
  base: "/console",
  mode: "history",
  routes: routes
});

router.beforeEach((to, from, next) => {
  const mode: BrowseMode = (to.params["browseMode"] as BrowseMode) ?? defaultMode;
  switch (mode) {
    case "songs":
      document.title = `${serviceName} | Query console (Artists)`;
      break;
    case "artists":
      document.title = `${serviceName} | Query console (Songs)`;
      break;
    default:
      document.title = `${serviceName} | Query console (${mode})`;
      break;
  }
  next();
});
