package org.dspace.layout.service.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for CrisLayoutBoxAccessServiceImpl box grants scenarios
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

@RunWith(MockitoJUnitRunner.class)
public class CrisLayoutBoxAccessServiceImplTest {

    @Mock
    private AuthorizeService authorizeService;
    @Mock
    private ItemService itemService;
    private CrisLayoutBoxAccessServiceImpl crisLayoutBoxAccessService;

    @Before
    public void setUp() throws Exception {
        crisLayoutBoxAccessService = new CrisLayoutBoxAccessServiceImpl(authorizeService, itemService);
    }

    /**
     * box with PUBLIC {@link LayoutSecurity} set, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void publicAccessReturnsTrue() throws SQLException {

        boolean granted =
            crisLayoutBoxAccessService.grantAccess(mock(Context.class),
                                                   ePerson(UUID.randomUUID()),
                                                   box(LayoutSecurity.PUBLIC),
                                                   mock(Item.class));

        assertThat(granted, is(true));
    }

    /**
     * box with OWNER_ONLY {@link LayoutSecurity} set, accessed by item's owner, access is granted.
     *
     * @throws SQLException
     */
    @Test
    public void ownerOnlyAccessedByItemOwner() throws SQLException {
        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(userUuid.toString());

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(mock(Context.class), ePerson(userUuid), box(LayoutSecurity.OWNER_ONLY), item);

        assertThat(granted, is(true));
    }

    /**
     * box with OWNER_ONLY {@link LayoutSecurity} set, accessed different owner, access forbidden.
     *
     * @throws SQLException
     */
    @Test
    public void ownerOnlyAccessedByOtherUser() throws SQLException {
        UUID userUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();

        Item item = mock(Item.class);


        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(ownerUuid.toString());

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(mock(Context.class), ePerson(userUuid), box(LayoutSecurity.OWNER_ONLY), item);

        assertThat(granted, is(false));
    }

    /**
     * box with OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by administrator user, grant given
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByAdminUser() throws SQLException {

        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(true);

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(context, mock(EPerson.class), box(LayoutSecurity.OWNER_AND_ADMINISTRATOR), item);

        assertThat(granted, is(true));
    }

    /**
     * box with OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by item's owner user, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByOwnerUser() throws SQLException {

        UUID userUuid = UUID.randomUUID();

        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);

        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(userUuid.toString());

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(context, ePerson(userUuid), box(LayoutSecurity.OWNER_AND_ADMINISTRATOR), item);

        assertThat(granted, is(true));
    }

    /**
     * box with OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by different user, access NOT granted
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByDifferentNotAdminUser() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();

        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);

        when(itemService.getMetadataFirstValue(item, "cris", "owner", null, Item.ANY))
            .thenReturn(ownerUuid.toString());

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(context, ePerson(userUuid), box(LayoutSecurity.OWNER_AND_ADMINISTRATOR), item);

        assertThat(granted, is(false));
    }

    /**
     * box with ADMINISTRATOR {@link LayoutSecurity} set, accessed by administrator eperson, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void adminAccessedByAdmin() throws SQLException {

        Context context = mock(Context.class);

        when(authorizeService.isAdmin(context)).thenReturn(true);

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(context, mock(EPerson.class), box(LayoutSecurity.ADMINISTRATOR), mock(Item.class));

        assertThat(granted, is(true));
    }

    /**
     * box with ADMINISTRATOR {@link LayoutSecurity} set, accessed by not administrator eperson, access is NOT  granted
     *
     * @throws SQLException
     */
    @Test
    public void adminAccessedByNotAdmin() throws SQLException {

        Context context = mock(Context.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(context, mock(EPerson.class), box(LayoutSecurity.ADMINISTRATOR), mock(Item.class));

        assertThat(granted, is(false));
    }

    /**
     * box with CUSTOM_DATA {@link LayoutSecurity} set, accessed by user with id having authority on metadata
     * contained in the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        List<MetadataValue> metadataValueList = Arrays.asList(metadataValueWithAuthority(userUuid.toString()),
                                                              metadataValueWithAuthority(UUID.randomUUID().toString()));

        MetadataField securityMetadataField = securityMetadataField();

        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(mock(Context.class), ePerson(userUuid), box(LayoutSecurity.CUSTOM_DATA,
                                                                         securityMetadataField), item);

        assertThat(granted, is(true));
    }


    /**
     * box with CUSTOM_DATA {@link LayoutSecurity} set, accessed by user belonging to a group with id having
     * authority on metadata contained in the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserGroupAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID securityAuthorityUuid = UUID.randomUUID();

        EPerson currentUser = ePerson(userUuid, UUID.randomUUID(), groupUuid);

        Item item = mock(Item.class);


        MetadataField securityMetadataField = securityMetadataField();

        List<MetadataValue> metadataValueList =
            Arrays.asList(metadataValueWithAuthority(securityAuthorityUuid.toString()),
                          metadataValueWithAuthority(groupUuid.toString()));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);


        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(mock(Context.class), currentUser, box(LayoutSecurity.CUSTOM_DATA, securityMetadataField),
                             item);

        assertThat(granted, is(true));
    }

    /**
     * box with CUSTOM_DATA {@link LayoutSecurity} set, accessed by user with id that does not have any authority on
     * metadata contained in the box, access is NOT  granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserNotAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID securityAuthorityUuid = UUID.randomUUID();

        Item item = mock(Item.class);


        MetadataField securityMetadataField = securityMetadataField();

        List<MetadataValue> metadataValueList =
            Collections.singletonList(metadataValueWithAuthority(securityAuthorityUuid.toString()));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted =
            crisLayoutBoxAccessService
                .grantAccess(mock(Context.class), ePerson(userUuid, groupUuid), box(LayoutSecurity.CUSTOM_DATA,
                                                                                    securityMetadataField), item);

        assertThat(granted, is(false));
    }

    private EPerson ePerson(UUID userUuid, UUID... groupsUuid) {
        EPerson currentUser = mock(EPerson.class);

        when(currentUser.getID()).thenReturn(userUuid);

        List<Group> grups = Arrays.stream(groupsUuid).map(this::group).collect(Collectors.toList());
        when(currentUser.getGroups())
            .thenReturn(grups);
        return currentUser;
    }

    private Group group(UUID groupUuid) {
        Group group = mock(Group.class);
        when(group.getID()).thenReturn(groupUuid);
        return group;
    }

    private MetadataField securityMetadataField() {
        MetadataSchema ms = mock(MetadataSchema.class);
        when(ms.getName()).thenReturn("schemaname");
        MetadataField msf = mock(MetadataField.class);
        when(msf.getMetadataSchema()).thenReturn(ms);
        when(msf.getElement()).thenReturn("element");
        return msf;
    }

    private MetadataValue metadataValueWithAuthority(String authority) {
        MetadataValue metadataValue = mock(MetadataValue.class);

        when(metadataValue.getAuthority()).thenReturn(authority);

        return metadataValue;

    }


    private CrisLayoutBox box(LayoutSecurity security, MetadataField... securityFields) {
        CrisLayoutBox box = new CrisLayoutBox();
        box.setSecurity(security);
        box.addMetadataSecurityFields(new HashSet<>(Arrays.asList(securityFields)));
        return box;
    }
}
