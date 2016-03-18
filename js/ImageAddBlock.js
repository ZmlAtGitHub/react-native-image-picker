/**
 * Created by lvbingru on 2/29/16.
 */

import {styles} from './ImageAddBlock.style.js'

import React, {InteractionManager, Component, PropTypes, View, Text, TouchableOpacity, Image} from 'react-native';
import Icon from 'react-native-vector-icons/FontAwesome';
import ImagePicker from './ImagePicker'

const propTypes = {
    list: PropTypes.array,
    onChanged : PropTypes.func,
    limit : PropTypes.number,
}

const defaultProps = {
    limit : 65535,
    list : [],
}

export default class ImageAddBlock extends Component {
    constructor(props) {
        super(props);

        this.state = {
        };
    }

    render() {
        const {list, limit, style} = this.props;
        const left = limit - this.props.list.length;

        return (
            <View style={[styles.container, style]}>
                {
                    list && list.map((el,index)=>{
                        const uri = this.getUri(el)
                        return(
                            <TouchableOpacity
                                key = {index}
                                style = {styles.listViewItem}
                                onPress = {()=>{
                                }}
                            >
                                <Image
                                    style = {styles.img}
                                    source = {{uri}}
                                />
                                <TouchableOpacity
                                  style = {styles.btnDel}
                                  onPress = {()=>{
                                    this.onDelete(index);
                                  }}
                                >
                                    <Icon
                                      name="trash"
                                      style={styles.del}
                                      size={17}
                                      color={'#b4b7b9'}
                                    />
                                </TouchableOpacity>
                            </TouchableOpacity>
                        )
                    })
                }
                {
                    left>0?
                      <TouchableOpacity
                        style = {[styles.listViewItem, styles.addItem]}
                        onPress={()=>{
                            this.props.navigator.push({params:{
                                onSelectFinished:this.onSelectFinished.bind(this),
                                limit:left,
                            }, component: ImagePicker})
                        }}
                      >
                          <Icon
                            name="plus"
                            style={styles.txtAdd}
                          />
                      </TouchableOpacity>:
                      null
                }
            </View>
        );
    }

    getUri(el) {
        if (el.node) {
            return el.node.image.uri
        }
        else {
            return el
        }
    }

    onSelectFinished(list) {
        const r = [...this.props.list, ...list];
        const {onChanged} = this.props;
        onChanged && onChanged(r);
    }

    onDelete(index) {
        const {onChanged, list} = this.props;
        const r = list.filter((el,i)=>{
            if (index === i) {
                return false;
            }
            return true;
        })
        onChanged && onChanged(r);
    }
}

ImageAddBlock.propTypes = propTypes;
ImageAddBlock.defaultProps = defaultProps;
