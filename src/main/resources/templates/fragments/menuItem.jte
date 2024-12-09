@param String link
@param boolean active
@param String title
@param String glyph
@param String text

<li class="nav-item">
    <a class="${active ? "nav-link active" : "nav-link"}" href="${link}" title="${title}">
        <span class="fa fa-${glyph}" class="fa fa-home"></span>
        <span>${text}</span>
    </a>
</li>
