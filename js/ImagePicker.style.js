/**
 * Created by lvbingru on 11/26/15.
 */

import {StyleSheet, Dimensions, Platform} from 'react-native';

const SCREEN_WIDTH = Dimensions.get('window').width;

const configFour = {
    paddingLeft : 7,
    margin : 3,
    size : (SCREEN_WIDTH - 7 * 2 - 3 * 2 * 4) / 4,
}

const cameraSize = (configFour.size + configFour.margin)*2;

export var styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: 'white',
    },

    navBar : {
        backgroundColor: 'rgba(0,0,0,0.8)',
        flexDirection: 'row',
        alignItems: 'stretch',
        height: Platform.OS === 'android' ? 44 : 64,
        paddingTop: Platform.OS === 'android' ? 0 : 20
    },

    navTitle : {
        flex : 1,
        justifyContent : 'center',
        alignItems : 'center',
    },

    navButton :{
        justifyContent : 'center',
        alignItems : 'center',
        width : 60,
    },

    txtNavButton :{
        color : 'white',
        fontSize: 14,
    },

    txtNavTitle :{
        color : 'white',
        fontSize: 17,
    },

    header : {
        flexDirection : 'row',
    },

    camera : {
        width : cameraSize,
        height : cameraSize,
        margin: configFour.margin,
        marginVertical : configFour.margin,
        alignItems : 'center',
        justifyContent : 'center',
        backgroundColor : 'gray',
    },

    fontCamera : {
        fontSize : 50,
        color : 'white',
    },

    headerItem :{
        flexDirection : 'row',
        flexWrap: 'wrap',
        width : cameraSize + configFour.margin * 2,
        height : cameraSize + configFour.margin * 2,
    },

    listView : {
        flexWrap : 'wrap',
        flexDirection : 'row',
        paddingLeft : configFour.paddingLeft,
        paddingTop: 10,
    },

    listViewItem : {
        width : configFour.size,
        height : configFour.size,
        marginHorizontal : configFour.margin,
        marginVertical : configFour.margin,
    },

    img : {
        width : configFour.size,
        height : configFour.size,
    },

    pickerBottom : {
        flexDirection : 'row',
        height : 48,
        alignItems : 'center',
        justifyContent : 'space-between',
        padding: 10,
        borderColor: '#d3d3d3',
        borderTopWidth: 1,
    },

    txtBottom :{
        textAlign: 'center',
        paddingHorizontal : 8,
        fontSize : 14,
        color : 'green',
    },

    viewNum: {
        width: 20,
        height: 20,
        borderRadius:10,
        backgroundColor : 'green',
        justifyContent : 'center',
        alignItems : 'center',
        overflow : 'hidden',
    },

    txtNum :{
        color : 'white',
    },

    bntBottom :{
        justifyContent : 'center',
        flexDirection : 'row',
        alignItems : 'center',
    },

    select : {
        position : 'absolute',
        right : 0,
        top : 0,
        backgroundColor : 'transparent',
    },
});