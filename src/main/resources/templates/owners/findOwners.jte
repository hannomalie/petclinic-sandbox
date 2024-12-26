@import org.springframework.samples.petclinic.system.Templating.CustomFields
@import org.springframework.validation.FieldError
@param CustomFields fields
@param String lastName
@template.fragments.layout(content = @`
!{var valid = !fields.hasErrors();}
<h2>Find Owners</h2>

<form action="/owners" method="get" class="form-horizontal" id="search-owner-form">
    <div class="form-group">
        <div class="control-group" id="lastNameGroup">
            <label class="col-sm-2 control-label">Last name </label>
            <div class="col-sm-10">
                <input class="form-control" id="lastName" name="lastName" value="${lastName}" size="30" maxlength="80" />
                <span class="help-inline">
                    @if(!valid)
                        <div>
                        @for(String error : fields.getFieldErrors())
                            <p>${error}</p>
                        @endfor
                        </div>
                    @endif
                  </span>
            </div>
        </div>
    </div>
    <div class="form-group">
        <div class="col-sm-offset-2 col-sm-10">
            <button type="submit" class="btn btn-primary" id="search-owner-form-submit">Find Owner</button>
        </div>
    </div>

    <a class="btn btn-primary" href="/owners/new">Add Owner</a>

</form>
`, menu = "owners")

