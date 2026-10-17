# Strings Translation Context

A translation reference for `app/src/main/res/values/strings.xml`.
Each row explains where the string is used in the app, so translators can
choose correct wording, especially for short words and phrases.
Placeholders such as `%1$s` / `%1$d` must be preserved.

## Basic

| Name | English translation | Context |
|---|---|---|
| `app_name` | Book's Story | The application name. **Not translatable.** |
| `default_string` | Default | Generic "Default" label; used for the reader font family option and the "Screen orientation" setting (via `ReaderScreenOrientation.DEFAULT`). |

## Screens

| Name | English translation | Context |
|---|---|---|
| `library_screen` | Library | Title of the Library tab (navigation bar, top bar). |
| `history_screen` | History | Title of the History tab (navigation bar, top bar). |
| `browse_screen` | Browse | Title of the Browse tab (file browser where books are added from the device). |
| `settings_screen` | Settings | Title of the Settings screen top bar and the bottom sheet menu item. |
| `about_screen` | About app | Title of the About screen and the bottom sheet menu item. |

## Dialog Labels (dialog titles)

| Name | English translation | Context |
|---|---|---|
| `add_books` | Add books? | Title of the confirmation dialog in Browse (via `BrowseAddDialog`) asking which scanned files to add to the Library. |
| `delete_books` | Delete books? | Title of the confirmation dialog when deleting multiple selected books in Library (via `LibraryDeleteDialog`). |
| `delete_book` | Delete book? | Title of the confirmation dialog when deleting a single book in Book Info (via `BookInfoDeleteDialog`). |
| `delete_history` | Delete reading history? | Title of the confirmation dialog for deleting the whole reading history (via `HistoryDeleteWholeHistoryDialog`). |
| `remove_category` | Remove category? | Title of the confirmation dialog for removing a library category in Settings → Library → Categories. |

## Dialog Descriptions

| Name | English translation | Context |
|---|---|---|
| `add_books_description` | You can choose which books you want to add. You can edit Title and Cover image of the book later. The loading process can take up to several minutes. | Body text of the Browse "Add books?" dialog. Mentions that Title/Cover can be edited later and adding may take minutes. |
| `delete_books_description` | It will delete all selected books(%1$s) from the database. It will not delete selected books from your device. | Body text of the Library delete dialog. `%1$s` is the number of selected books. Clarifies only the database record is removed. |
| `delete_book_description` | It will delete this book from the database. It will not delete this book from your device. | Body text of the single-book delete dialog in Book Info. |
| `delete_history_description` | It will delete the whole history from the database. After deleting the whole history, the order of the books will be reset. | Body text of the "Delete reading history?" dialog. Warns that the custom book ordering (based on history) resets. |
| `remove_category_description` | This will remove this category from the Library but will not delete any books. | Body text of the category removal dialog; books stay in the Library. |

## Errors

| Name | English translation | Context |
|---|---|---|
| `error_no_description` | No description. | Placeholder text in Book Info when the book has no description. |
| `error_something_went_wrong` | Something went wrong. | Generic error shown in the Browse "Add books" dialog for files that failed to parse (via `NullableBook.Null`). |
| `error_no_translator` | No translator app found. | Toast in the Reader when the "Translate" action finds no app handling the translate intent. |
| `error_no_dictionary` | No dictionary app found. | Toast in the Reader when the "Dictionary" action finds no app handling the dictionary intent. |
| `error_no_browser` | No browser app found. | Toast when a web link cannot be opened (About screen, Reader web search). |
| `error_no_share_app` | No app to share found. | Toast when the share intent (text, exported book, image) has no receiving app. |
| `error_no_file_manager_app` | No app to choose folder found. | Toast in Browse scan settings when no file manager app is installed to pick a folder. |
| `error_could_not_get_text` | File is empty, missing, or inaccessible. Verify the file, scan settings or change the path, then try again. | Full-screen reader error (via `ReaderErrorPlaceholder`) when the book text cannot be loaded; offers the "Change path" action. |
| `error_could_not_reset_cover` | Could not reset cover image. | Toast in Book Info when resetting the cover to the default fails. |
| `error_closed_source` | Closed source | Label shown in the Licenses list (via `LicensesItem`) next to libraries without open-source licenses. |
| `error_could_not_export` | Could not export book with images. | Toast when exporting an FB2 book with embedded generated images fails. |
| `error_no_file` | File does not exist or inaccessible | Error shown under the file path field in Book Info → File details (via `BookInfoDetailsBottomSheetItem`) when the book file cannot be found. |
| `error_field_empty` | This field should not be empty. | Supporting error text of the "Create category" dialog when the title field is empty. |

## Is Empty (empty-state messages)

| Name | English translation | Context |
|---|---|---|
| `search_field_empty` | Search… | Placeholder of the search text fields on Library, History and Browse screens. |
| `browse_empty` | Expand the library's book collection with your downloads | Empty placeholder of the Browse tab; has a "Set up scanning" action button. |
| `library_empty` | Read your favorite books and sort them between categories | Empty placeholder of the Library tab; has an "Add a book" action button. |
| `history_empty` | Let your last book you read open the pages of a new great story of your reading journey | Empty placeholder of the History tab (no action button). |
| `categories_empty` | No categories. | Text inside the "Move" dialogs (Library and Book Info) when no categories exist yet. |

## Query Strings (formatted)

| Name | English translation | Context |
|---|---|---|
| `selected_items_count_query` | %1$s selected | Title of the selection top bar on Library and Browse screens. `%1$s` is the number of selected items. |
| `color_preset_query` | Preset %1$s | Fallback name of an unnamed color preset. `%1$s` is the preset id. Used in Appearance → Colors and the "Selected preset" toast. |
| `color_preset_selected_query` | Selected «%1$s» | Toast after switching a color preset. `%1$s` is the preset name (or `color_preset_query` fallback). |

## Action Texts (buttons / menu items)

| Name | English translation | Context |
|---|---|---|
| `cancel` | Cancel | Generic dialog cancel button (via `Dialog`). |
| `add_book` | Add a book | Action button of the Library empty placeholder, opens Browse. |
| `move` | Move | Dialog title and action for moving selected books (or one book) to categories. |
| `delete` | Delete | Generic delete action; also the reader image context menu item. |
| `never` | Never | Value shown in Book Info → File details → "Last opened" when the book was never opened. |
| `undo` | Undo | Snackbar action in History to restore a just-deleted history entry. |
| `copy` | Copy | Crash screen button and text-selection context menu item (copies selection to clipboard). |
| `report` | Report | Crash screen button that shares the crash log. |
| `translate` | Translate | Reader text-selection context menu item opening a translator app. |
| `dictionary` | Dictionary | Reader text-selection context menu item opening a dictionary app. |
| `generate_image` | Generate image | Reader text-selection context menu item starting AI illustration generation for the selection. |
| `set_cover_image` | Set as book cover | Context menu item on generated images in the Reader (long-press menu, via `ReaderLayoutTextImage`). |
| `next` | Next | Button of the onboarding (Start) screens advancing to the next step. |
| `done` | Done | Button of the final onboarding screen finishing the setup. |
| `web_search` | Web search | Reader text-selection context menu item searching the selection on the web. |
| `share` | Share | Reader text-selection context menu item and generated-image context menu item. |
| `ok` | OK | Generic dialog confirmation button (via `Dialog`). |
| `change_path` | Change path | Action of the reader error placeholder; opens Book Info with the path-edit dialog opened (via `BookInfoScreen.changePathChannel`). |
| `add_folder` | Add folder | Action in Settings → Browse → Scan (and onboarding scan step) adding a folder to scan for books. |
| `edit` | Edit | Generic edit action; secondary dialog action opening category editing; also dialog title for edit dialogs. |
| `create_category` | Create category | Action and dialog title in Settings → Library → Categories for creating a new category. |
| `set_up_scanning` | Set up scanning | Action button of the Browse empty placeholder, opens Browse settings (scan section). |

## Export

| Name | English translation | Context |
|---|---|---|
| `export_with_images` | Export | Button in Book Info actions; exported only for FB2 books (generated images can only be embedded back into FB2). |
| `export_with_images_desc` | Export the FB2 file with all generated images embedded | Semantics of the export action (documentation string for the FB2-only export button). |

## Book

| Name | English translation | Context |
|---|---|---|
| `unknown_author` | Unknown | Author label when the file metadata has no author; used by `BookMapper`, parsers and the author-edit dialog fallback. |
| `reset_cover` | Reset cover image | Item in Book Info "Change cover" bottom sheet restoring the original cover. |
| `reset_cover_desc` | Reset cover image of the book to the default | Description of the reset-cover item. |
| `change_cover` | Change cover image | Item in Book Info "Change cover" bottom sheet opening the photo picker. |
| `change_cover_desc` | Choose new cover from your photos | Description of the change-cover item. |
| `delete_cover` | Delete cover image | Item in Book Info "Change cover" bottom sheet removing the current cover (shown only if a cover exists). |
| `delete_cover_desc` | Delete current cover image of the book | Description of the delete-cover item. |
| `author` | Author | Accessibility label / field name for the book author. |

## Tabs

| Name | English translation | Context |
|---|---|---|
| `default_tab` | Default | Title of the implicit "no category" tab in Library tabs. |
| `general_tab` | General | First tab of the Reader settings bottom sheet. |
| `display_tab` | Display | "Display" tab in the Browse filter bottom sheet. |
| `color_tab` | Colors | "Colors" tab of the Reader settings bottom sheet. |
| `ai_tab` | AI | "AI" tab of the Reader settings bottom sheet. |
| `filter_tab` | Filter | "Filter" tab in the Browse filter bottom sheet. |
| `sort_tab` | Sort | "Sort" tab in filter bottom sheets (Library and Browse). |
| `reader_tab` | Reader | "Reader" tab of the Reader settings bottom sheet. |

## Settings Categories

| Name | English translation | Context |
|---|---|---|
| `general_settings` | General | Settings list item and screen title: language, double-back-exit. |
| `general_settings_desc` | General | Subtitle/description of the General settings list item ("App language"). |
| `appearance_settings` | Appearance | Settings list item and screen title: theme, colors. |
| `appearance_settings_desc` | Theme, colors | Subtitle of the Appearance settings list item. |
| `reader_settings` | Reader | Settings list item and screen title: font, text, images, etc. |
| `reader_settings_desc` | Font, text | Subtitle of the Reader settings list item. |
| `browse_settings` | Browse | Settings list item and screen title: scanning, display, sort, filter. |
| `browse_settings_desc` | Scan, display | Subtitle of the Browse settings list item. |
| `library_settings` | Library | Settings list item and screen title: categories, display, tabs. |
| `library_settings_desc` | Categories, display, tabs | Subtitle of the Library settings list item. |

## Settings Subcategories (section headers)

| Name | English translation | Context |
|---|---|---|
| `font_reader_settings` | Font | Reader settings section header: font family, thickness, style, size, letter spacing. |
| `text_reader_settings` | Text | Reader settings section header: alignment, line height, paragraph spacing/indentation. |
| `translator_reader_settings` | Translator | Reader settings section header: double-click translation. |
| `padding_reader_settings` | Margins | Reader settings section header: side/vertical margins, cutout, bottom bar margin. |
| `misc_reader_settings` | Misc | Reader settings section header: fullscreen, keep screen on, hide bars on fast scroll. |
| `reading_speed_reader_settings` | Reading speed | Reader settings section header: highlight words, perception expander, horizontal limiter. |
| `reading_mode_reader_settings` | Reading mode | Reader settings section header: horizontal gesture options. |
| `system_reader_settings` | System | Reader settings section header: custom brightness, screen orientation. |
| `chapters_reader_settings` | Chapters | Reader settings section header: chapter title alignment. |
| `images_reader_settings` | Images | Reader settings section header: display images, captions, size, alignment, color effects. |
| `progress_reader_settings` | Progress | Reader settings section header: progress count, progress bar options. |
| `theme_appearance_settings` | Theme preferences | Appearance settings section header: dark theme, app theme, contrast, pure dark. |
| `colors_appearance_settings` | Colors | Appearance settings section header: color presets. |
| `scan_browse_settings` | Scan | Browse settings section header: scanned folders. |
| `display_settings` | Display | Section header in Library/Browse settings: layout, grid size, etc. |
| `tabs_settings` | Tabs | Library settings section header: category tabs options. |
| `categories_settings` | Categories | Library settings section header: category management. |
| `filter_browse_settings` | Filter | Browse settings section header: file filter. |
| `sort_settings` | Sort | Section header in Library/Browse settings: sort order. |
| `ai_reader_settings` | AI assistant | Reader settings / bottom sheet section header: models, API key, proxy, characters. |

## Settings Notes (explanatory paragraphs)

| Name | English translation | Context |
|---|---|---|
| `browse_scan_option_note` | These folders are used to scan and retrieve files in the Browse. Removing a folder here will not delete actual files or books from the Library. To revoke access, click the corresponding button. | Note under the scanned-folders list in Settings → Browse → Scan. |
| `library_categories_option_note` | Initially, books are added to the default category, but can belong to several custom categories created here. Removing a category here will not delete books from the Library. Hold and move the category icon to rearrange the categories. | Note under the categories management list. |
| `library_per_category_sort_option_note` | Sorting can be changed in the Library. | Note under the "Per-category sort" switch. |

## Settings Options — General

| Name | English translation | Context |
|---|---|---|
| `language_option` | App language | General settings option listing available app languages. |
| `dark_theme_option` | Dark theme | Appearance option: Disabled / Enabled / System. |
| `pure_dark_option` | Pure dark | Appearance option shown when dark theme is on. |
| `absolute_dark_option` | Absolute dark | Switch making the background completely black (shown when Pure Dark is active). |
| `absolute_dark_option_desc` | Change background color to be completely black | Description of the Absolute dark switch. |
| `theme_contrast_option` | Theme contrast | Appearance option: Standard / Medium / High (only for themes supporting contrast). |
| `app_theme_option` | App theme | Appearance option showing the theme previews (Mercury, Neptune, …). |
| `double_press_exit_option` | Double back press to exit | Switch requiring pressing back twice to exit the app. |
| `double_press_exit_option_desc` | Press back twice to exit the app | Description of the double-back-press switch. |
| `show_navigation_labels_option` | Show navigation labels | Switch showing labels under the navigation bar icons. |
| `show_navigation_labels_option_desc` | Show navigation bar labels under icons | Description of the navigation labels switch. |

## Settings Options — Reader (Font & Text)

| Name | English translation | Context |
|---|---|---|
| `font_family_option` | Font family | Chips list of available fonts (via `ReaderData.fonts`). |
| `font_thickness_option` | Font thickness | Chips list of font weights. |
| `font_style_option` | Font style | Segmented buttons: Normal / Italic. |
| `font_size_option` | Font size | Slider for the text size. |
| `line_height_option` | Line height | Slider for the spacing between lines. |
| `paragraph_height_option` | Paragraph spacing | Slider for vertical spacing between paragraphs; also adds top/bottom padding around the reader list. |
| `paragraph_indentation_option` | Paragraph indentation | Slider for the first-line indent (hidden for Center/End alignment). |
| `color_preset_option` | Color preset | Header of the color presets list in Appearance → Colors. |
| `background_color_option` | Background color | Label of the background color picker of a color preset. |
| `font_color_option` | Font color | Label of the font color picker of a color preset. |
| `side_padding_option` | Side margins | Slider for horizontal padding of the reader text. |
| `vertical_padding_option` | Vertical margins | Slider for vertical padding of the reader text. |
| `double_click_translation_option` | Double click translation | Switch enabling translation on double-tap of a paragraph. |
| `double_click_translation_option_desc` | Double click on paragraph to translate it | Description of the double-click translation switch. |
| `fast_color_preset_change_option` | Fast color preset change | Switch enabling swiping the reader top bar to change color preset. |
| `fast_color_preset_change_option_desc` | Swipe top bar left or right to change color preset | Description of the fast preset change switch. |
| `text_alignment_option` | Text alignment | Segmented buttons: Start / Justify / Center / End. |
| `chapter_title_alignment_option` | Chapter title alignment | Segmented buttons for the chapter heading alignment. |
| `letter_spacing_option` | Letter spacing | Slider for spacing between letters. |
| `cutout_padding_option` | Cutout margin | Switch applying padding around the display cutout (notch). |
| `cutout_padding_option_desc` | Apply margin to the cutout area | Description of the cutout margin switch. |
| `fullscreen_option` | Fullscreen | Switch hiding system bars and margins in the Reader. |
| `fullscreen_option_desc` | Hide system bars and margins | Description of the fullscreen switch. |
| `keep_screen_on_option` | Keep screen on | Switch preventing the screen from turning off while reading. |
| `hide_bars_on_fast_scroll_option` | Hide bars on fast scroll | Switch hiding reader bars during fast scrolling. |
| `perception_expander_option` | Perception expander | Speed-reading switch (borders around the center focus zone). |
| `perception_expander_option_desc` | Increase your reading speed by focusing on center | Description of the perception expander switch. |
| `perception_expander_padding_option` | Line side margin | Slider for the side margin of the perception expander lines. |
| `perception_expander_thickness_option` | Line thickness | Slider for the thickness of the perception expander lines. |
| `horizontal_limiter_option` | Horizontal limiter | Speed-reading switch (horizontal ruler + screen dimming). |
| `horizontal_limiter_option_desc` | Show horizontal ruler and dim unfocused part of the screen | Description of the horizontal limiter switch. |
| `horizontal_limiter_height_option` | Height | Slider for the height of the horizontal limiter. |
| `horizontal_limiter_vertical_offset_option` | Vertical offset | Slider for the vertical position of the horizontal limiter. |
| `horizontal_limiter_ruler_option` | Horizontal ruler | Switch showing the limiter ruler line. |
| `horizontal_limiter_ruler_thickness_option` | Ruler thickness | Slider for the ruler line thickness. |
| `horizontal_limiter_dimming_option` | Dimming | Slider for how much of the unfocused screen area is dimmed. |
| `screen_orientation_option` | Screen orientation | Chips list of orientation choices (via `ReaderScreenOrientation`). |
| `custom_screen_brightness_option` | Custom brightness | Switch enabling a reader-specific screen brightness. |
| `screen_brightness_option` | Brightness | Slider for the custom brightness value. |
| `horizontal_gesture_option` | Horizontal gesture | Chips list: Off / On / On (Inverse) — swipe gestures for page progression. |
| `horizontal_gesture_scroll_option` | Scroll fraction | Slider: how far the page scrolls per horizontal gesture. |
| `horizontal_gesture_sensitivity_option` | Sensitivity | Slider for the gesture sensitivity. |
| `horizontal_gesture_alpha_anim_option` | Visibility animation | Switch animating page visibility during the gesture. |
| `horizontal_gesture_alpha_anim_option_desc` | Enable visibility animation when using horizontal gesture | Description of the visibility animation switch. |
| `horizontal_gesture_pull_anim_option` | Pull animation | Switch animating the page pull during the gesture. |
| `horizontal_gesture_pull_anim_option_desc` | Enable pull animation when using horizontal gesture | Description of the pull animation switch. |
| `horizontal_gesture_disable_scrolling_option` | Disable scrolling | Switch completely disabling reader scrolling (only gesture navigation). |
| `horizontal_gesture_disable_scrolling_option_desc` | Completely disable reader scrolling | Description of the disable-scrolling switch. |
| `bottom_bar_padding_option` | Bottom bar margin | Slider for additional bottom padding of the reader bottom bar. |
| `highlighted_reading_option` | Highlight words | Speed-reading switch highlighting the beginning of each word. |
| `highlighted_reading_option_desc` | Highlight the beginning of each word | Description of the highlight-words switch. |
| `highlighted_reading_thickness_option` | Highlight thickness | Slider: how strongly word beginnings are highlighted. |
| `images_option` | Images | Switch displaying images inside books. |
| `images_option_desc` | Display images | Description of the images switch. |
| `images_captions_option` | Captions | Switch displaying captions under images. |
| `images_captions_option_desc` | Display captions under images | Description of the captions switch. |
| `images_corners_roundness_option` | Corners roundness | Slider for image corner rounding. |
| `images_alignment_option` | Alignment | Segmented buttons for image alignment (Start / Center / End). |
| `images_width_option` | Size | Slider for image width as a percentage of the screen. |
| `images_color_effects_option` | Color effects | Chips list of image color filters (via `ReaderColorEffects`). |
| `progress_bar_option` | Progress bar | Switch showing a progress bar at the bottom of the reader. |
| `progress_bar_option_desc` | Display progress bar at the bottom | Description of the progress bar switch. |
| `progress_bar_padding_option` | Margins | Slider for the progress bar padding. |
| `progress_bar_alignment_option` | Alignment | Segmented buttons for the progress bar alignment. |
| `progress_bar_font_size_option` | Font size | Slider for the progress bar text size. |
| `progress_count_option` | Progress count | Segmented buttons: Percentage / Quantity. |

## Settings Options — AI

| Name | English translation | Context |
|---|---|---|
| `api_key_option` | API key | Text field title for the OpenRouter API key (hidden in the reader bottom sheet). |
| `api_key_option_label` | OpenRouter API key | Field label/hint of the API key input. |
| `image_model_option` | Image model | Text field title for the image generation model. |
| `image_model_option_label` | OpenRouter image model slug | Field label of the image model input (a model slug identifier). |
| `image_style_option` | Style block | Multi-line text field title for the image prompt style block. |
| `image_style_option_label` | Style block for image prompt | Field label of the style block input. |
| `style_reference_option` | Style reference | Switch attaching a collage of the book's existing illustrations as a style reference to the image request. |
| `style_reference_option_desc` | Attach a collage of the book's illustrations as a style reference | Description of the style reference switch. |
| `proxy_option` | Proxy | Switch enabling a network proxy for image generation. |
| `proxy_option_desc` | HTTP or SOCKS5 proxy | Description of the proxy switch. |
| `proxy_protocol_option` | Proxy protocol | Chips list: HTTP / SOCKS5. |
| `proxy_host_option` | Proxy host | Text field for the proxy host address. |
| `proxy_host_option_label` | e.g. 127.0.0.1 | Field hint of the proxy host input. |
| `proxy_port_option` | Proxy port | Text field for the proxy port. |
| `proxy_port_option_label` | e.g. 1080 | Field hint of the proxy port input. |
| `text_assistant_settings` | AI assistant | Legacy/duplicate section title for the AI assistant settings. |
| `text_assistant_model_option` | Text Model | Text field title for the text assistant model. |
| `text_assistant_model_option_label` | OpenRouter smart text model slug | Field label of the text assistant model input. |
| `text_assistant_preferred_latency_option` | Preferred latency (p90, sec) | Text field: maximum provider latency at p90 for the text assistant, in seconds. |
| `text_assistant_preferred_latency_option_label` | Preferred maximum provider latency at p90 for text assistant | Field label of the text assistant latency input. |
| `text_assistant_max_input_price_option` | Max input price ($/1M) | Text field: maximum prompt token price for the text assistant, in USD per million tokens. |
| `text_assistant_max_input_price_option_label` | Maximum prompt token price for text assistant | Field label of the text assistant price input. |
| `summarize_model_option` | Summarize Model | Text field title for the model used for characters extraction. |
| `summarize_model_option_label` | OpenRouter model slug used for characters extraction | Field label of the summarize model input. |
| `text_assistant_max_tokens_option` | Max input tokens | Text field: model context limit used to chunk long books. |
| `text_assistant_max_tokens_option_label` | Model context limit used to chunk long books | Field label of the max tokens input. |
| `character_preferred_latency_option` | Preferred latency (p90, sec) | Text field: maximum provider latency at p90 for characters extraction, in seconds. |
| `character_preferred_latency_option_label` | Preferred maximum provider latency at p90 for characters extraction | Field label of the characters latency input. |
| `character_max_input_price_option` | Max input price ($/1M) | Text field: maximum prompt token price for characters extraction, in USD per million tokens. |
| `character_max_input_price_option_label` | Maximum prompt token price for characters extraction | Field label of the characters price input. |
| `characters_reader_settings` | Characters | Section header of the characters group in the AI tab (also the notification channel name). |
| `analyze_characters` | Analyze book characters | Action starting the character extraction (reads the whole book and builds a characters catalog). |
| `characters_generating` | Extracting characters… | Shown while extraction is running (settings row label and the progress notification channel name). |
| `characters_empty` | No characters yet. Analyze the book content to build a characters catalog. | Text shown when the characters catalog is empty and extraction is not running. |
| `characters_prompt` | Read the entire book text, ignoring any image references. Identify the protagonist and all major and significant recurring characters. For each character, synthesize a consistent visual description from the entire text. | The system prompt sent to the AI model for characters extraction (kept in sync with the raw prompt resource). |
| `characters_processing` | Processing | Prefix in the extraction progress notification, followed by the percentage and download dots (e.g. "Processing 42%"). |
| `characters_failed` | Character extraction failed | Title of the extraction error notification. |
| `characters_stopped_by_system` | Character extraction was stopped by the system. | Failure message when the system time limit (Android 15+ `onTimeout`) cancelled the extraction. |
| `edit_description` | Edit description | Item in the character entry long-press popup menu (Characters section of the AI tab); opens the description editing dialog. |
| `load_picture` | Load picture | Item in the character entry long-press popup menu; opens the photo picker to choose a character portrait, then the crop dialog. |
| `paste` | Paste picture | Item in the character entry long-press popup menu; pastes an image from the clipboard as the character portrait. Enabled only when the clipboard holds an image. |
| `no_image_clipboard` | No image in clipboard. | Toast when the paste-picture action is used but the clipboard contains no image. |
| `character_image_saved` | Character picture saved. | Toast after the adjusted portrait was saved for the character. |
| `portrait_crop_title` | Adjust picture | Title of the round-viewport portrait crop dialog (shown after loading/pasting a character picture). |
| `portrait_crop_desc` | Zoom and pan the picture inside the circle. | Description of the portrait crop dialog, explaining the zoom/pan gesture inside the circular viewport. |
| `error_could_not_decode` | Couldn't decode the selected image. | Error text inside the portrait crop dialog when the selected image cannot be decoded. |
| `image_generating` | Generating image… | Title of the image generation progress notification and the notification channel name. |
| `image_step_prepare` | Preparing context… | Progress notification text for the context preparation step of image generation. |
| `image_step_collage` | Building style collage… | Progress notification text for the style reference collage step. |
| `image_step_distill` | Distilling scene… | Progress notification text for the scene description step. |
| `image_step_generate` | Rendering illustration… | Progress notification text for the final image rendering step. |
| `image_generation_failed` | Image generation failed | Title of the image generation error notification. |
| `image_stopped_by_system` | Image generation was stopped by the system. | Failure message when the system time limit cancelled the generation. |
| `image_already_generating` | An image is already being generated. | Toast when trying to start another generation while one is running, or deleting its image. |
| `retry` | Retry | Action of the error notifications restarting the failed extraction/generation step. |

## Settings Options — Library

| Name | English translation | Context |
|---|---|---|
| `read_button_option` | Show «Read» button | Switch showing the play ("Read") button on library book items. |
| `show_progress_option` | Show progress | Switch showing the reading progress badge on library book items. |
| `title_position_option` | Title position | Chips list for grid items: Hidden / Below / Inside. |
| `show_book_count_option` | Show book count | Switch showing the number of books next to the Library title / in tabs. |
| `show_category_tabs_option` | Show category tabs | Switch showing category tabs under the Library top bar. |
| `always_show_default_tab_option` | Always show default tab | Switch always including the "Default" (no category) tab. |
| `per_category_sort_option` | Per-category sort | Switch allowing a separate sort order per category. |

## Color Presets

| Name | English translation | Context |
|---|---|---|
| `red_color` | Red | Label of the red channel slider in the color picker of a color preset. |
| `green_color` | Green | Label of the green channel slider in the color picker. |
| `blue_color` | Blue | Label of the blue channel slider in the color picker. |
| `color_preset_placeholder` | Name a preset… | Placeholder of the unnamed color preset name field in Appearance → Colors. |

## Dark Theme Properties (enum values)

| Name | English translation | Context |
|---|---|---|
| `dark_theme_off` | Disabled | `DarkTheme.OFF` segmented button value. |
| `dark_theme_on` | Enabled | `DarkTheme.ON` segmented button value. |
| `dark_theme_follow_system` | System | `DarkTheme.FOLLOW_SYSTEM` segmented button value. |

## Pure Dark Properties (enum values)

| Name | English translation | Context |
|---|---|---|
| `pure_dark_off` | Disabled | `PureDark.OFF` segmented button value. |
| `pure_dark_on` | Enabled | `PureDark.ON` segmented button value. |
| `pure_dark_power_saver` | Saver | `PureDark.SAVER` value: pure dark only in power saving mode. |

## Theme Contrast Properties (enum values)

| Name | English translation | Context |
|---|---|---|
| `theme_contrast_standard` | Standard | `ThemeContrast.STANDARD` segmented button value. |
| `theme_contrast_medium` | Medium | `ThemeContrast.MEDIUM` segmented button value. |
| `theme_contrast_high` | High | `ThemeContrast.HIGH` segmented button value. |

## Font Style Properties (enum values)

| Name | English translation | Context |
|---|---|---|
| `font_style_italic` | Italic | Italic font style segmented button value. |
| `font_style_normal` | Normal | Normal font style segmented button value. |

## Library Title Position (enum values)

| Name | English translation | Context |
|---|---|---|
| `library_title_position_off` | Hidden | Title is not displayed on grid items. |
| `library_title_position_below` | Below | Title is displayed below the cover on grid items. |
| `library_title_position_inside` | Inside | Title is overlaid on the cover on grid items. |

## Library Categories

| Name | English translation | Context |
|---|---|---|
| `library_categories_create_category_placeholder` | Title | Field label of the "Create category" dialog input. |

## Layout (enum values)

| Name | English translation | Context |
|---|---|---|
| `layout_option` | Display mode | Option title choosing the list layout. |
| `layout_list` | List | `BrowseLayout.LIST` / `LibraryLayout.LIST` segmented button value. |
| `layout_grid` | Grid | `BrowseLayout.GRID` / `LibraryLayout.GRID` segmented button value. |

## Sort Order (enum values)

| Name | English translation | Context |
|---|---|---|
| `sort_order_name` | Alphabetically | Sort books/files by name. |
| `sort_order_file_format` | File format | Browse files sort by format. |
| `sort_order_last_modified` | Last modified | Browse files sort by modification date. |
| `sort_order_file_size` | File size | Browse files sort by size. |
| `sort_order_last_read` | Last read | Library books sort by last read time. |
| `sort_order_progress` | Progress | Library books sort by reading progress. |
| `sort_order_author` | Author | Library books sort by author. |

## Grid Size

| Name | English translation | Context |
|---|---|---|
| `grid_size_option` | Grid size | Slider for the number of grid columns (0 = auto). |
| `grid_size_auto` | Auto | Placeholder shown when the grid size is automatic (slider at 0). |
| `grid_size_per_row` | per row | Suffix of the grid size slider value, e.g. "5 per row". |

## Alignment (enum values)

| Name | English translation | Context |
|---|---|---|
| `alignment_start` | Start | Alignment value: beginning side (used for text, chapter titles, images, progress bar). |
| `alignment_justify` | Justify | Text alignment value: justified text. |
| `alignment_center` | Center | Alignment value: center. |
| `alignment_end` | End | Alignment value: end side. |

## Screen Orientation (enum values)

| Name | English translation | Context |
|---|---|---|
| `screen_orientation_free` | Free | Reader orientation: `SCREEN_ORIENTATION_USER` (follows device rotation). |
| `screen_orientation_portrait` | Portrait | Reader orientation: user portrait. |
| `screen_orientation_landscape` | Landscape | Reader orientation: user landscape. |
| `screen_orientation_locked_portrait` | Locked portrait | Reader orientation: locked portrait. |
| `screen_orientation_locked_landscape` | Locked landscape | Reader orientation: locked landscape. |

## Horizontal Gesture (enum values)

| Name | English translation | Context |
|---|---|---|
| `horizontal_gesture_off` | Off | Horizontal gesture disabled. |
| `horizontal_gesture_on` | On | Horizontal gesture enabled (swipe advances forward). |
| `horizontal_gesture_inverse` | On (Inverse) | Horizontal gesture enabled with inverted direction. |

## Highlighted Reading

| Name | English translation | Context |
|---|---|---|
| `highlighted_reading_level` | level | Suffix of the highlight thickness slider value, e.g. "2 level". |

## Color Effects (enum values)

| Name | English translation | Context |
|---|---|---|
| `color_effects_off` | Off | Image color effects disabled. |
| `color_effects_grayscale` | Grayscale | Images rendered grayscale. |
| `color_effects_font` | Font color | Images tinted with the reader font color. |
| `color_effects_background` | Background color | Images blended with the reader background color. |

## Font Thickness (enum values)

| Name | English translation | Context |
|---|---|---|
| `font_thickness_thin` | Thin | `FontWeight.Thin` chip value. |
| `font_thickness_extra_light` | Extra light | `FontWeight.ExtraLight` chip value. |
| `font_thickness_light` | Light | `FontWeight.Light` chip value. |
| `font_thickness_normal` | Normal | `FontWeight.Normal` chip value. |
| `font_thickness_medium` | Medium | `FontWeight.Medium` chip value. |

## Progress Count (enum values)

| Name | English translation | Context |
|---|---|---|
| `progress_count_percentage` | Percentage | Progress displayed as a percent. |
| `progress_count_quantity` | Quantity | Progress displayed as pages/quantity. |

## Themes (preview names)

| Name | English translation | Context |
|---|---|---|
| `dynamic_theme` | Mercury | Name of the Material You dynamic color theme (Android 12+). |
| `blue_theme` | Neptune | Name of the blue color theme. |
| `green_theme` | Earth | Name of the green color theme. |
| `green2_theme` | Io | Name of the second green color theme. |
| `green_gray_theme` | Enceladus | Name of the green-gray color theme. |
| `red_theme` | Mars | Name of the red color theme. |
| `red_gray_theme` | Callisto | Name of the red-gray color theme. |
| `purple_theme` | Jupiter | Name of the purple color theme. |
| `purple_gray_theme` | Ceres | Name of the purple-gray color theme. |
| `pink_theme` | Saturn | Name of the pink color theme. |
| `pink2_theme` | Ganymede | Name of the second pink color theme. |
| `lavender_theme` | Eris | Name of the lavender color theme. |
| `marsh_theme` | Pluto | Name of the marsh color theme. |
| `yellow_theme` | Venus | Name of the yellow color theme. |
| `yellow2_theme` | Makemake | Name of the second yellow color theme. |
| `aqua_theme` | Uranus | Name of the aqua color theme. |
| `gray_theme` | Iapetus | Name of the gray color theme. |

## Toast Messages

| Name | English translation | Context |
|---|---|---|
| `press_again_toast` | Press back again to exit | Toast when double-back-press-to-exit is enabled and the first back press happened. |
| `books_added` | All selected books were successfully added. | Toast after books were added to the Library from Browse. |
| `books_moved` | All selected books were successfully moved. | Toast after moving selected books to categories. |
| `books_deleted` | All selected books were successfully deleted. | Toast after deleting selected books. |
| `cover_image_changed` | Cover Image was successfully changed. | Toast after changing the cover via the photo picker. |
| `cover_image_deleted` | Cover Image was successfully deleted. | Toast after deleting the cover. |
| `book_deleted` | This book was successfully deleted. | Toast after deleting a single book in Book Info. |
| `book_moved` | This book was successfully moved. | Toast after moving a single book to categories. |
| `copied` | Copied | Toast confirming the copy to clipboard (Android ≤ 12 only; newer versions show the system indicator). |
| `history_deleted` | History was successfully deleted. | Toast after deleting the whole reading history. |
| `cover_reset` | Cover image was successfully reset. | Toast after resetting the cover to the original. |
| `title_changed` | Title was successfully changed. | Toast after editing the book title. |
| `author_changed` | Author was successfully changed. | Toast after editing the book author. |
| `description_changed` | Description was successfully changed. | Toast after editing the book description. |
| `path_changed` | File path was successfully changed. | Toast after editing the book file path. |
| `cover_image_set` | Book cover was successfully set. | Toast after setting a generated image as the book cover. |

## Snackbar Messages

| Name | English translation | Context |
|---|---|---|
| `history_element_deleted` | History element was successfully deleted. | Snackbar message in History with the "Undo" action. |

## Book Info

| Name | English translation | Context |
|---|---|---|
| `start_reading` | Start | Reader button label when the book has no reading progress. |
| `continue_reading_query` | Continue from %1$s | Reader button label when there is progress. `%1$s` is the progress percentage, e.g. "Continue from 15%". |
| `file_details` | File details | Title of the file details bottom sheet and the accessibility label of its open button. |
| `file_path` | Path | Label of the editable file path field in the file details sheet. |
| `file_last_opened` | Last opened | Label of the "Last opened" field (value is a formatted date or "Never"). |
| `file_size` | Size | Label of the file size field (formatted as KB/MB or "Unknown"). |
| `unknown` | Unknown | Value of the file size field when the size cannot be determined. |

## Reader

| Name | English translation | Context |
|---|---|---|
| `no_chapters` | No chapters | Reader top bar subtitle when the book has no chapters. |
| `chapters` | Title of the chapters drawer and the accessibility label of its open button. | Title of the chapters drawer and the accessibility label of its open button. |
| `ai_illustrations` | AI illustrations | Section header in the Reader settings bottom sheet listing the book's generated images (tapping scrolls to the image). |

## About

| Name | English translation | Context |
|---|---|---|
| `app_version_option` | App version | About list item opening the releases page. |
| `report_bug_option` | Report a bug | About list item opening the issues page. |
| `licenses_option` | Open source licenses | About list item opening the licenses screen (also its top bar title). |
| `credits_option` | Credits | About list item opening the credits screen (also its top bar title). |
| `help_translate_option` | Help translate | About list item opening the translation (Weblate) page. |
| `support_development_option` | Support development | About list item opening the support/donations page. |
| `contributors_option` | Contributors | About list item opening the contributors page. |

## Badges

| Name | English translation | Context |
|---|---|---|
| `reddit_badge` | Reddit | Social badge accessibility label in About. **Not translatable.** |
| `github_badge` | GitHub | Social badge accessibility label in About. **Not translatable.** |
| `gitlab_badge` | GitLab | Social badge accessibility label in About. **Not translatable.** |
| `codeberg_badge` | Codeberg | Social badge accessibility label in About. **Not translatable.** |

## Credits

| Name | English translation | Context |
|---|---|---|
| `credits_design` | Design | Credit tag for design contributions (via `CreditData`). |
| `credits_icon` | Icons | Credit tag for icon assets. |
| `credits_updates` | New updates managing | Credit tag for update management contributions. |
| `credits_fonts` | Fonts | Credit tag for font assets. |
| `credits_translation` | Translation | Credit tag for translation contributions. |
| `credits_contribution` | Contribution | Credit tag for general contributions. |
| `credits_ideas` | Ideas | Credit tag for idea contributions. |

## Start (Onboarding)

| Name | English translation | Context |
|---|---|---|
| `start_welcome` | Welcome to %1$s! | Onboarding welcome headline. `%1$s` is the application name. |
| `start_welcome_desc` | To a world where words charm, teach and lead on incredible adventures! Let's set some things up! | Onboarding welcome description under the headline. |
| `start_language_preferences` | Language preferences | Section header of the language selection step in onboarding. |
| `start_theme_preferences` | Theme preferences | Section header of the theme step in onboarding. |
| `start_scan_preferences` | Scan preferences | Section header of the scan setup step in onboarding. |
| `start_done` | Everything is done! | Final onboarding screen headline. |
| `start_done_desc` | Setup completed. | Final onboarding screen description under the headline. |

## History

| Name | English translation | Context |
|---|---|---|
| `today` | Section header grouping history entries read today (via `GroupedHistory.title`). | Section header grouping history entries read today. |
| `yesterday` | Yesterday | Section header grouping history entries read yesterday. |

## Crash

| Name | English translation | Context |
|---|---|---|
| `crash_title` | Oops! The app crashed. | Headline of the crash screen. |
| `crash_description` | Please report this crash message in the most convenient way for you. | Description of the crash screen (Copy / Report buttons below). |

## Content Descriptions (accessibility)

| Name | English translation | Context |
|---|---|---|
| `continue_reading_content_desc` | Continue reading | Accessibility label of the "Read" play button on library book items. |
| `go_back_content_desc` | Go back | Accessibility label of the back button. |
| `cover_image_content_desc` | Cover image | Default accessibility label of the book cover image component (`AsyncCoverImage`). |
| `open_reader_settings_content_desc` | Reader settings | Accessibility label of the settings button in the reader top bar. |
| `file_icon_content_desc` | File | Accessibility label of the file icon on browse list/grid items. |
| `sort_order_content_desc` | Sort order | Accessibility label of the ascending/descending arrow icon in sort option rows. |
| `checkbox_content_desc` | Checkbox | Accessibility label of the circular checkbox component. |
| `cover_image_not_found_content_desc` | Cover image not found | Accessibility label of the placeholder icon when a book has no cover. |
| `error_content_desc` | Error | Accessibility label of the error icon on failed parse items in the Browse add dialog. |
| `search_content_desc` | Search | Accessibility label of the search icon button in top bars. |
| `clear_selected_items_content_desc` | Clear selected items | Accessibility label of the button clearing the item selection. |
| `add_files_content_desc` | Add files | Accessibility label of the button opening the Browse add dialog. |
| `select_all_files_content_desc` | Select all files | Accessibility label of the select-all button in the Browse selection top bar. |
| `select_all_books_content_desc` | Select all books | Accessibility label of the select-all button in the Library selection top bar. |
| `exit_search_content_desc` | Exit search | Accessibility label of the back button closing the search field. |
| `delete_whole_history_content_desc` | Delete whole history | Accessibility label of the broom icon button in the History top bar. |
| `move_books_content_desc` | Move selected books | Accessibility label of the move button in the Library selection top bar. |
| `delete_books_content_desc` | Delete selected books | Accessibility label of the delete button in the Library selection top bar. |
| `revert_content_desc` | Revert | Accessibility label of the revert (history) button next to color channel sliders. |
| `selected_content_desc` | Selected | Accessibility label of the checkmark shown on selected items (theme previews, segmented buttons). |
| `app_icon_content_desc` | App icon | Accessibility label of the app icon on the About screen. |
| `delete_history_element_content_desc` | Delete history element | Accessibility label of the delete button on history entries. |
| `library_content_desc` | Your library | Accessibility label of the Library tab in the navigation bar/rail. |
| `history_content_desc` | Reading history | Accessibility label of the History tab in the navigation bar/rail. |
| `browse_content_desc` | Add books | Accessibility label of the Browse tab in the navigation bar/rail. |
| `arrow_content_desc` | Arrow | Accessibility label of arrow icons (generic). |
| `reset_start_content_desc` | Reset start guide | Accessibility label of the button in Settings reopening the onboarding. |
| `open_in_web_content_desc` | Open in web | Accessibility label of the web button on the license info screen. |
| `delete_color_preset_content_desc` | Delete color preset | Accessibility label of the delete button for a color preset. |
| `create_color_preset_content_desc` | Create new color preset | Accessibility label of the add button creating a color preset. |
| `shuffle_color_preset_content_desc` | Shuffle colors | Accessibility label of the shuffle button randomizing a preset's colors. |
| `drag_content_desc` | Drag | Accessibility label of the drag handle (reorderable items, category icon). |
| `filter_content_desc` | Filter | Accessibility label of the filter icon button in top bars. |
| `checkpoint_back_content_desc` | Checkpoint back | Accessibility label of the back checkpoint arrow in the reader bottom bar. |
| `checkpoint_forward_content_desc` | Checkpoint forward | Accessibility label of the forward checkpoint arrow in the reader bottom bar. |
| `chapters_content_desc` | Chapters | Accessibility label of the chapters (menu) button in the reader top bar. |
| `more_content_desc` | More | Accessibility label of the "more options" button opening the navigator bottom sheet. |
| `pin_content_desc` | Pin | Accessibility label of the pin button on browse layout headers. |
| `show_less_content_desc` | Show less | Accessibility label of the collapse arrow on the book description in Book Info. |
| `show_more_content_desc` | Show more | Accessibility label of the expand arrow on the book description in Book Info. |
| `edit_content_desc` | Edit | Accessibility label of edit buttons (author, category, file path). |
| `expand_content_desc` | Expand | Accessibility label of the expand arrow on nested chapters. |
| `collapse_content_desc` | Collapse | Accessibility label of the collapse arrow on nested chapters. |
| `remove_content_desc` | Remove | Accessibility label of remove buttons (scanned folders, categories). |
| `note_content_desc` | Note | Accessibility label of the info icon in settings notes. |
