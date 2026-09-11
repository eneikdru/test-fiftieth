module.exports = {
  darkMode: "class",
  content: [
    "./*.html",
    "./src/**/*.{html,js,svelte}"
  ],
  theme: {
    extend: {
      colors: {
        "primary": "#003f87",
        "on-primary": "#ffffff",
        "background": "#f7f9fb",
        "on-surface": "#191c1e",
        "on-surface-variant": "#424752",
        "outline": "#727784",
        "outline-variant": "#c2c6d4",
        "surface-container-lowest": "#ffffff",
        "surface-container": "#eceef0",
        "surface-variant": "#e0e3e5",
        "error": "#ba1a1a",
        "error-container": "#ffdad6",
        "on-error-container": "#93000a"
      },
      fontFamily: {
        "sans": ["Inter", "sans-serif"],
        "heading": ["Manrope", "sans-serif"]
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/container-queries')
  ],
}
