<script>
    import { onMount } from 'svelte';

    let searchQuery = '';
    let documents = [];
    let isLoading = false;
    let searchError = '';

    const apiBaseUrl = '/api/v1';

    async function handleSearch() {
        isLoading = true;
        searchError = '';
        try {
            const params = new URLSearchParams();
            if (searchQuery.trim()) params.append('query', searchQuery.trim());

            // Setting a large size to fetch all or we can just fetch 10
            params.append('page', '0');
            params.append('size', '50');

            const url = `${apiBaseUrl}/documents/search?${params.toString()}`;
            const response = await fetch(url);

            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }

            const data = await response.json();

            if (data && Array.isArray(data.results)) {
                documents = data.results;
            } else if (Array.isArray(data)) {
                documents = data;
            } else {
                documents = [];
            }
        } catch (err) {
            console.error('Failed to fetch documents', err);
            searchError = 'Failed to load documents';
            documents = [];
        } finally {
            isLoading = false;
        }
    }

    onMount(() => {
        handleSearch();
    });

    function getFileName(filePath) {
        if (!filePath) return 'document.pdf';
        return filePath.substring(filePath.lastIndexOf('/') + 1) || 'document.pdf';
    }

    // Function to calculate approx time ago - just mock for UI purposes or display year
    function getTimeAgo(doc) {
        return "recently";
    }

    // Size mock
    function getMockSize(doc) {
        return "1.2 MB";
    }
</script>

<div class="antialiased w-full h-screen flex flex-col" style="background-color: #f7f9fb; color: #191c1e; font-family: Inter, sans-serif;">
    <!-- TopAppBar -->
    <header class="bg-[#f7f9fb] text-[#000000] font-bold docked full-width top-0 border-b border-[#c6c6cd] flat no shadows transition-colors duration-200 flex justify-between items-center w-full px-4 h-16 sticky z-10">
        <div class="flex items-center gap-4">
            <button aria-label="Menu" class="hover:bg-[#eceef0] rounded-full p-2 transition-colors duration-200 flex items-center justify-center">
                <span class="material-symbols-outlined" data-icon="menu" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">menu</span>
            </button>
            <span class="text-[20px] leading-[28px] font-semibold">Discovery</span>
        </div>
        <button aria-label="Account" class="hover:bg-[#eceef0] rounded-full p-2 transition-colors duration-200 flex items-center justify-center">
            <span class="material-symbols-outlined" data-icon="account_circle" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">account_circle</span>
        </button>
    </header>

    <!-- Main Content Canvas -->
    <main class="flex-1 overflow-y-auto px-4 pb-24 pt-4 flex flex-col gap-8 hide-scrollbar">
        <!-- Search Area -->
        <section class="flex flex-col gap-2 sticky top-0 bg-[#f7f9fb] pt-2 pb-4 z-10">
            <div class="relative w-full">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-[#76777d]" data-icon="search" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">search</span>
                <input
                    aria-label="Search documents"
                    class="w-full bg-[#f2f4f6] border border-[#c6c6cd] rounded-xl py-3 pl-12 pr-12 text-[14px] leading-[20px] font-normal text-[#191c1e] focus:outline-none focus:border-[#000000] focus:ring-1 focus:ring-[#000000] transition-all shadow-sm placeholder:text-[#76777d] placeholder:font-normal"
                    placeholder="Search documents..."
                    type="text"
                    bind:value={searchQuery}
                    on:keyup={(e) => e.key === 'Enter' && handleSearch()}
                />
                <button
                    aria-label="Filter"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-[#45464d] hover:text-[#000000] transition-colors flex items-center justify-center"
                    on:click={handleSearch}
                >
                    <span class="material-symbols-outlined" data-icon="tune" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">tune</span>
                </button>
            </div>

            <div class="flex gap-2 overflow-x-auto hide-scrollbar pb-1">
                <button class="shrink-0 bg-[#f2f4f6] text-[#191c1e] border border-[#c6c6cd] rounded-full px-4 py-1.5 text-[12px] leading-[16px] tracking-[0.05em] font-medium whitespace-nowrap hover:bg-[#eceef0] transition-colors">Recent</button>
                <button class="shrink-0 bg-[#f2f4f6] text-[#191c1e] border border-[#c6c6cd] rounded-full px-4 py-1.5 text-[12px] leading-[16px] tracking-[0.05em] font-medium whitespace-nowrap hover:bg-[#eceef0] transition-colors">PDFs</button>
                <button class="shrink-0 bg-[#f2f4f6] text-[#191c1e] border border-[#c6c6cd] rounded-full px-4 py-1.5 text-[12px] leading-[16px] tracking-[0.05em] font-medium whitespace-nowrap hover:bg-[#eceef0] transition-colors">Contracts</button>
                <button class="shrink-0 bg-[#f2f4f6] text-[#191c1e] border border-[#c6c6cd] rounded-full px-4 py-1.5 text-[12px] leading-[16px] tracking-[0.05em] font-medium whitespace-nowrap hover:bg-[#eceef0] transition-colors">Q3 Reports</button>
            </div>
        </section>

        <!-- Results List -->
        <section class="flex flex-col gap-4">
            <h2 class="text-[20px] leading-[28px] font-semibold text-[#191c1e] mb-2">Results ({documents.length})</h2>

            {#if isLoading}
                <div class="flex items-center justify-center py-8">
                    <div class="animate-spin rounded-full h-8 w-8 border-4 border-[#000000] border-t-transparent"></div>
                </div>
            {:else if searchError}
                <div class="text-[#ba1a1a] py-4 text-center">{searchError}</div>
            {:else if documents.length === 0}
                <div class="text-[#45464d] py-4 text-center">No documents found.</div>
            {:else}
                {#each documents as doc}
                    <a href="/api/v1/documents/{doc.id || doc.fileName || doc.title}/view" target="_blank" rel="noopener noreferrer" class="block focus:outline-none focus:ring-2 focus:ring-[#000000] focus:ring-offset-2 rounded-xl">
                    <article tabindex="0" role="button" aria-label="View document" class="bg-[#ffffff] border border-[#c6c6cd] rounded-xl p-4 flex gap-4 hover:bg-[#f2f4f6] transition-colors cursor-pointer group focus:outline-none focus:ring-2 focus:ring-[#000000] focus:ring-offset-2">
                        <div class="flex-shrink-0 flex items-start pt-1">
                            <span class="material-symbols-outlined text-[#515f74]" data-icon="description" style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;">
                                {#if doc.filePath && doc.filePath.endsWith('.docx')}
                                    contract
                                {:else if doc.filePath && doc.filePath.endsWith('.xlsx')}
                                    table
                                {:else if doc.filePath && doc.filePath.endsWith('.zip')}
                                    image
                                {:else}
                                    description
                                {/if}
                            </span>
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex justify-between items-start gap-2 mb-1">
                                <h3 class="text-[20px] leading-[28px] font-semibold text-[#191c1e] truncate group-hover:text-[#000000] transition-colors">
                                    {getFileName(doc.filePath) || doc.title}
                                </h3>
                                <button aria-label="More options" class="text-[#45464d] hover:bg-[#e0e3e5] rounded-full p-1 -mt-1 -mr-1 transition-colors flex-shrink-0">
                                    <span class="material-symbols-outlined text-[20px]" data-icon="more_vert" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">more_vert</span>
                                </button>
                            </div>
                            <p class="text-[14px] leading-[20px] font-normal text-[#45464d] line-clamp-2 mb-2">
                                {doc.title} - {doc.authorOrganization || 'Unknown Author'} ({doc.publicationYear || ''})
                            </p>
                            <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px] leading-[16px] font-normal text-[#76777d]">
                                <span class="flex items-center gap-1">
                                    <span class="material-symbols-outlined text-[14px]" data-icon="schedule" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">schedule</span>
                                    {getTimeAgo(doc)}
                                </span>
                                <span>•</span>
                                <span>{getMockSize(doc)}</span>
                                <span>•</span>
                                <span class="bg-[#eceef0] px-2 py-0.5 rounded text-[#45464d]">Document</span>
                            </div>
                        </div>
                    </article>
                    </a>
                {/each}
            {/if}
        </section>
    </main>

    <!-- Footer / Imprint -->
    <footer class="w-full py-4 px-6 text-center text-xs text-[#76777d] border-t border-[#c6c6cd] flex justify-between items-center mb-20 bg-[#f7f9fb]">
        <span>Российский научно-исследовательский институт эпидемиологии</span>
        <button on:click={() => alert('Выходные данные (Imprint / Impressum):\nФБУН «НИИ Эпидемиологии»\nг. Москва, ул. Новогиреевская, 3А')} class="text-[#003f87] underline font-semibold hover:opacity-80">
            Imprint / Impressum
        </button>
    </footer>

    <!-- BottomNavBar -->
    <nav class="bg-[#ffffff] text-[#000000] fixed bottom-0 left-0 w-full z-50 border-t border-[#c6c6cd] flex justify-around items-center h-20 pb-safe px-4">
        <button aria-current="page" aria-label="Search" class="flex flex-col items-center justify-center bg-[#d5e3fd] text-[#57657b] rounded-full px-4 py-1 hover:bg-[#e0e3e5] scale-95 transition-transform">
            <span class="material-symbols-outlined mb-1" data-icon="search" style="font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;">search</span>
            <span class="text-[12px] leading-[16px] tracking-[0.05em] font-medium">Search</span>
        </button>
        <button aria-label="Recent" class="flex flex-col items-center justify-center text-[#45464d] hover:bg-[#e0e3e5] scale-95 transition-transform px-4 py-1 rounded-full">
            <span class="material-symbols-outlined mb-1" data-icon="history" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">history</span>
            <span class="text-[12px] leading-[16px] tracking-[0.05em] font-medium">Recent</span>
        </button>
        <button aria-label="Saved" class="flex flex-col items-center justify-center text-[#45464d] hover:bg-[#e0e3e5] scale-95 transition-transform px-4 py-1 rounded-full">
            <span class="material-symbols-outlined mb-1" data-icon="bookmark" style="font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;">bookmark</span>
            <span class="text-[12px] leading-[16px] tracking-[0.05em] font-medium">Saved</span>
        </button>
    </nav>
</div>

<style>
    .hide-scrollbar::-webkit-scrollbar { display: none; }
    .hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
    .line-clamp-2 {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }
</style>
