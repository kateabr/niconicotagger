// eslint-disable-next-line @typescript-eslint/no-var-requires
const path = require("path");

module.exports = {
  pages: {
    events: {
      title: "NicoNicoTagger | Loading...",
      entry: "src/main/typescript/pages/events/main.ts"
    },
    tags: {
      title: "NicoNicoTagger | Loading...",
      entry: "src/main/typescript/pages/tags/main.ts"
    },
    console: {
      title: "NicoNicoTagger | Loading...",
      entry: "src/main/typescript/pages/console/main.ts"
    },
    login: {
      title: "NicoNicoTagger | Login",
      entry: "src/main/typescript/pages/login/main.ts"
    },
    index: {
      title: "NicoNicoTagger | Login",
      entry: "src/main/typescript/pages/login/main.ts"
    }
  },

  configureWebpack: {
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "src/main/typescript")
      }
    }
  },

  lintOnSave: false,

  css: {
    sourceMap: true
  },

  devServer: {
    proxy: {
      "^/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
        pathRewrite: { "^/api": "/api" },
        logLevel: "debug"
      }
    }
  }
};
