<script>
  // Delivery Pipeline Component
</script>

<!-- TopAppBar (Mobile) / SideNav (Desktop) -->
<header class="flex items-center justify-between px-margin-mobile h-16 w-full fixed top-0 z-50 bg-surface border-b border-outline-variant md:hidden">
  <div class="flex items-center gap-sm">
    <span class="material-symbols-outlined text-primary" aria-hidden="true" style="font-variation-settings: 'FILL' 1;">account_tree</span>
    <h1 class="font-headline-lg-mobile text-headline-lg-mobile font-bold text-primary">Delivery Pipeline</h1>
  </div>
  <button aria-label="Notifications" class="material-symbols-outlined text-on-surface-variant focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary rounded">notifications</button>
</header>

<aside class="hidden md:flex flex-col w-64 bg-surface border-r border-outline-variant fixed top-0 left-0 h-full z-40 pt-lg px-md gap-lg">
  <div class="flex items-center gap-sm px-2">
    <span class="material-symbols-outlined text-primary" aria-hidden="true" style="font-variation-settings: 'FILL' 1;">account_tree</span>
    <h1 class="font-headline-lg-mobile text-headline-lg-mobile font-bold text-primary">Delivery Pipeline</h1>
  </div>
  <nav class="flex flex-col gap-2">
    <a href="#" class="flex items-center gap-3 px-4 py-3 bg-primary-container text-on-primary-container rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary" aria-current="page">
      <span class="material-symbols-outlined" aria-hidden="true" style="font-variation-settings: 'FILL' 1;">format_list_bulleted</span>
      <span class="font-body-md font-medium">Pipeline</span>
    </a>
    <a href="#" class="flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-surface-container-lowest rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary transition-colors">
      <span class="material-symbols-outlined" aria-hidden="true">merge_type</span>
      <span class="font-body-md font-medium">Branches</span>
    </a>
    <a href="#" class="flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-surface-container-lowest rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary transition-colors">
      <span class="material-symbols-outlined" aria-hidden="true">inventory_2</span>
      <span class="font-body-md font-medium">Components</span>
    </a>
    <a href="#" class="flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:bg-surface-container-lowest rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary transition-colors">
      <span class="material-symbols-outlined" aria-hidden="true">settings</span>
      <span class="font-body-md font-medium">Settings</span>
    </a>
  </nav>
</aside>

<!-- Main Content Canvas -->
<main class="pt-[80px] md:pt-margin-desktop md:pl-[calc(16rem+32px)] px-margin-mobile md:pr-margin-desktop flex flex-col gap-lg w-full max-w-7xl mx-auto">
  <!-- Header Section -->
  <section class="flex flex-col gap-xs">
    <h2 class="font-display text-[28px] md:text-[36px] md:leading-[44px] leading-[36px] font-bold tracking-tight text-on-surface">Feature Delivery: R1</h2>
    <div class="flex items-center gap-sm bg-primary/10 w-fit px-3 py-1.5 rounded-full border border-primary/20" role="status" aria-label="Status: R1 Implementation Complete">
      <span class="material-symbols-outlined text-primary text-[18px]" aria-hidden="true" style="font-variation-settings: 'FILL' 1;">check_circle</span>
      <span class="font-body-md text-body-md text-on-surface font-medium">R1 Implementation: Complete</span>
    </div>
  </section>

  <!-- Main Branch Delivery Card -->
  <section class="bg-surface-container-lowest rounded-xl border border-outline-variant shadow-[0_4px_12px_rgba(0,0,0,0.03)] p-md flex flex-col gap-md transition-all duration-200 hover:border-outline hover:shadow-[0_4px_16px_rgba(0,0,0,0.06)]" aria-labelledby="delivery-card-title">
    <div class="flex justify-between items-center border-b border-outline-variant/50 pb-sm">
      <h3 id="delivery-card-title" class="font-title-md text-title-md text-on-surface">Main Branch Delivery</h3>
      <span class="font-label-caps text-label-caps bg-primary-container text-on-primary-container px-2 py-0.5 rounded flex items-center gap-1" aria-label="Status: Delivered">
        <span class="w-1.5 h-1.5 rounded-full bg-primary block" aria-hidden="true"></span>
        DELIVERED
      </span>
    </div>

    <div class="flex items-center justify-center py-lg relative" aria-hidden="true">
      <!-- Visual Merge Representation -->
      <div class="flex flex-col gap-xl w-full max-w-[240px]">
        <!-- Source Branch -->
        <div class="flex items-center justify-between relative z-10">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-full border-2 border-secondary bg-surface flex items-center justify-center shadow-sm">
              <span class="material-symbols-outlined text-secondary text-[16px]">commit</span>
            </div>
            <span class="font-code-sm text-code-sm text-secondary px-2 py-1 bg-surface-container-low rounded border border-outline-variant/30">feature/r1</span>
          </div>
        </div>

        <!-- Merge Path (SVG) -->
        <svg class="absolute top-[32%] left-[16px] w-[24px] h-[40px] text-primary" fill="none" viewBox="0 0 24 40" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 0 V20 C12 30, 24 30, 24 40" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"></path>
          <path d="M20 36 L24 40 L28 36" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" transform="translate(-4, 0)"></path>
        </svg>

        <!-- Target Branch -->
        <div class="flex items-center justify-between relative z-10 pl-6">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-full border-2 border-primary bg-primary/10 flex items-center justify-center shadow-sm">
              <span class="material-symbols-outlined text-primary text-[16px]" style="font-variation-settings: 'FILL' 1;">merge</span>
            </div>
            <span class="font-code-sm text-code-sm text-primary font-bold px-2 py-1 bg-primary/5 rounded border border-primary/20">main</span>
          </div>
        </div>

        <!-- Decorative Line -->
        <div class="absolute left-[30px] top-[16px] bottom-[16px] w-[2px] bg-outline-variant/30 z-0"></div>
      </div>
    </div>
  </section>

  <!-- Delivered Components Section -->
  <section class="flex flex-col gap-md" aria-labelledby="components-title">
    <h3 id="components-title" class="font-title-md text-title-md text-on-surface">Delivered Components</h3>
    <div class="flex flex-col gap-sm">
      <!-- Component Item -->
      <button class="w-full text-left bg-surface-container-lowest rounded-lg border border-outline-variant p-sm flex justify-between items-center hover:bg-surface-container-low transition-colors cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary" aria-label="View UI Component Library version 2.4.0">
        <div class="flex items-center gap-sm">
          <div class="w-10 h-10 rounded bg-surface-variant flex items-center justify-center" aria-hidden="true">
            <span class="material-symbols-outlined text-on-surface-variant">widgets</span>
          </div>
          <div class="flex flex-col">
            <span class="font-body-md text-body-md font-semibold text-on-surface">UI Component Library</span>
            <span class="font-label-caps text-label-caps text-on-surface-variant">v2.4.0</span>
          </div>
        </div>
        <div class="flex flex-col items-end gap-1">
          <span class="font-label-caps text-label-caps bg-surface-container text-on-surface-variant px-2 py-0.5 rounded border border-outline-variant/50">On Main Branch</span>
          <span class="material-symbols-outlined text-primary text-[16px]" aria-hidden="true">check</span>
        </div>
      </button>
    </div>
  </section>
</main>

<!-- BottomNavBar (Mobile Only) -->
<nav class="md:hidden fixed bottom-0 w-full z-50 flex justify-around items-center px-4 py-2 bg-surface border-t border-outline-variant" aria-label="Mobile Navigation">
  <!-- Pipeline (Active) -->
  <a href="#" class="flex flex-col items-center justify-center bg-primary-container text-on-primary-container rounded-full px-4 py-1 active:scale-90 transition-all duration-200 focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary" aria-current="page">
    <span class="material-symbols-outlined" aria-hidden="true" style="font-variation-settings: 'FILL' 1;">format_list_bulleted</span>
    <span class="font-label-caps text-label-caps mt-1">Pipeline</span>
  </a>
  <!-- Branches (Inactive) -->
  <a href="#" class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-lowest active:scale-90 transition-all duration-200 px-4 py-1 rounded-full focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary">
    <span class="material-symbols-outlined" aria-hidden="true">merge_type</span>
    <span class="font-label-caps text-label-caps mt-1">Branches</span>
  </a>
  <!-- Components (Inactive) -->
  <a href="#" class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-lowest active:scale-90 transition-all duration-200 px-4 py-1 rounded-full focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary">
    <span class="material-symbols-outlined" aria-hidden="true">inventory_2</span>
    <span class="font-label-caps text-label-caps mt-1">Components</span>
  </a>
  <!-- Settings (Inactive) -->
  <a href="#" class="flex flex-col items-center justify-center text-on-surface-variant hover:bg-surface-container-lowest active:scale-90 transition-all duration-200 px-4 py-1 rounded-full focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary">
    <span class="material-symbols-outlined" aria-hidden="true">settings</span>
    <span class="font-label-caps text-label-caps mt-1">Settings</span>
  </a>
</nav>