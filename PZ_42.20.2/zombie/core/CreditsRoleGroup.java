// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import generation.builders.validation.TranslationKeyValidator;
import java.util.List;
import zombie.UsedFromLua;

@UsedFromLua
public enum CreditsRoleGroup {
    EXECUTIVES_AND_LEADERSHIP(
        CreditsRole.HEAD_RACCOON,
        CreditsRole.FOUNDERS,
        CreditsRole.TECHNICAL_DIRECTOR,
        CreditsRole.ART_DIRECTOR,
        CreditsRole.DESIGN_DIRECTOR,
        CreditsRole.PRODUCTION_DIRECTOR
    ),
    MUSIC_BY(CreditsRole.MUSIC_ORIGINALLY_COMPOSED_BY),
    PROGRAMMING_TEAM(CreditsRole.LEAD_TECHNICAL_PROGRAMMER, CreditsRole.LEAD_GAMEPLAY_PROGRAMMER, CreditsRole.ENGINEERS, CreditsRole.ADDITIONAL_CODE),
    CREATIVE_TEAM(
        CreditsRole.ARTISTS,
        CreditsRole.ENVIRONMENTAL_ARTISTS,
        CreditsRole.CONCEPT_ARTIST,
        CreditsRole.GRAPHIC_DESIGNER,
        CreditsRole.MARKETING_ARTIST,
        CreditsRole.ADDITIONAL_ARTISTS,
        CreditsRole.WRITER
    ),
    RESEARCH_AND_DEVELOPMENT(CreditsRole.RESEARCH_AND_DEVELOPMENT),
    PRODUCTION(CreditsRole.PRODUCER, CreditsRole.ASSOCIATE_PRODUCER),
    QUALITY_ASSURANCE(CreditsRole.LEAD_QA_TIS, CreditsRole.SENIOR_QA, CreditsRole.QUALITY_ASSURANCE_TESTERS),
    ADMINISTRATION(
        CreditsRole.HR,
        CreditsRole.SYSTEM_ADMINISTRATOR,
        CreditsRole.JUNIOR_SYSTEM_ADMINISTRATOR,
        CreditsRole.BACKUP_SYSADMIN,
        CreditsRole.TECH_SUPPORT,
        CreditsRole.LOCALISATION_LEAD,
        CreditsRole.LEAD_COMMUNITY_MANAGER,
        CreditsRole.COMMUNITY_MANAGERS,
        CreditsRole.COMMUNITY_MODERATORS,
        CreditsRole.OPERATIONS_MANAGER,
        CreditsRole.FINANCE
    ),
    VERTEX_BREAK(
        "media/ui/Logos/VertexBreak_credits.png",
        CreditsRole.SENIOR_SOFTWARE_DEVELOPER,
        CreditsRole.MIDDLE_SOFTWARE_DEVELOPER,
        CreditsRole.LEAD_QA,
        CreditsRole.QA
    ),
    SOUND_ARRIVAL(
        "media/ui/Logos/Arrival_credits.png",
        CreditsRole.LEAD_SOUND_DESIGNER_ARRIVAL,
        CreditsRole.TECHNICAL_SOUND_DESIGNER_ARRIVAL,
        CreditsRole.SOUND_SUPERVISOR,
        CreditsRole.SOUND_DESIGNER,
        CreditsRole.ASSOCIATE_SOUND_DESIGNER,
        CreditsRole.INTERACTIVE_MUSIC_COMPOSER,
        CreditsRole.SENIOR_PRODUCER,
        CreditsRole.HEAD_PRODUCER
    ),
    GENERAL_ARCADE("media/ui/Logos/GeneralArcade_credits.png", CreditsRole.GENERAL_ARCADE),
    TEA(
        "media/ui/Logos/TEA_credits.png",
        CreditsRole.DIRECTOR,
        CreditsRole.LEAD_PROGRAMMER,
        CreditsRole.SENIOR_PROGRAMMER,
        CreditsRole.RENDERING_PROGRAMMER,
        CreditsRole.UI_DESIGNER,
        CreditsRole.QUALITY_ASSURANCE
    ),
    CONTRIBUTORS(CreditsRole.WIKI_ADMIN, CreditsRole.WIKI_EDITORS, CreditsRole.CONTRIBUTORS, CreditsRole.SPECIAL_INFECTED, CreditsRole.SPECIAL_THANKS),
    LOCALIZATION(CreditsRole.LOCALIZATION),
    TMG(
        CreditsRole.TMG_UA,
        CreditsRole.TMG_PT,
        CreditsRole.TMG_DA,
        CreditsRole.TMG_DE,
        CreditsRole.TMG_HU,
        CreditsRole.TMG_FI,
        CreditsRole.TMG_CN,
        CreditsRole.TMG_CH,
        CreditsRole.TMG_IT,
        CreditsRole.TMG_KO,
        CreditsRole.TMG_NL,
        CreditsRole.TMG_NO,
        CreditsRole.TMG_PL
    ),
    TOOLS(),
    IN_LOVING_MEMORY_OF(CreditsRole.IN_LOVING_MEMORY_OF);

    private final String title = "credits_role_group.%s".formatted(this.name().toLowerCase());
    private final String logo;
    private final List<CreditsRole> roles;

    private CreditsRoleGroup(final CreditsRole... roles) {
        this(null, roles);
    }

    private CreditsRoleGroup(final String logo, final CreditsRole... roles) {
        this.logo = logo;
        this.roles = List.of(roles);
    }

    public static List<CreditsRoleGroup> getAll() {
        return List.of(values());
    }

    public String getTitle() {
        return this.title;
    }

    public String getLogo() {
        return this.logo;
    }

    public List<CreditsRole> getRoles() {
        return this.roles;
    }

    static {
        if (Core.IS_DEV) {
            for (CreditsRoleGroup entry : values()) {
                TranslationKeyValidator.of(entry.title);
            }
        }
    }
}
